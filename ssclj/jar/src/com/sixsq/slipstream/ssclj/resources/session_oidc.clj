(ns com.sixsq.slipstream.ssclj.resources.session-oidc
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.spec.session]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-oidc :as tpl]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.cyclone :as auth-oidc]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.auth.utils.sign :as sg]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.utils.timestamp :as tsutil]
    [environ.core :as environ]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [ring.util.codec :as codec]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.auth.external :as ex]))

(def ^:const authn-method "oidc")

(def ^:const login-request-timeout (* 3 60))

(def ^:const oidc-relative-url "/auth?response_type=code&client_id=%s&redirect_uri=%s")

(def ^:const validation-url "https://nuv.la/api/session/uuid/validate")


;;
;; schemas
;;

(def SessionDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc SessionDescription)

;;
;; utils
;;

(defn oidc-client-info
  []
  (let [client-id (environ/env :oidc-client-id)
        base-url (environ/env :oidc-base-url)
        public-key (environ/env :oidc-public-key)]
    [client-id base-url public-key]))

(defn throw-bad-client-config []
  (log-util/log-and-throw 500 "missing client ID, base URL, or public key (::oidc-client-id, :oidc-base-url, :oidc-public-key) for OIDC authentication"))

(defn throw-missing-oidc-code []
  (log-util/log-and-throw 400 "OIDC authentication callback request does not contain required code"))

(defn throw-no-access-token []
  (log-util/log-and-throw 400 "unable to retrieve OIDC access token"))

(defn throw-no-user-info []
  (log-util/log-and-throw 400 "unable to retrieve or decrypt OIDC user information"))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/session-template.oidc-create))
(defmethod p/create-validate-subtype authn-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into session resource
;;
(defn create-session
  "Creates a new session resource from the users credentials and the request
   header. The result contains the authentication method, the user's identifier,
   the client's IP address, and the virtual host being used. NOTE: The expiry
   is not included and MUST be added afterwards."
  [{:keys [username]} headers]
  (let [server (:slipstream-ssl-server-hostname headers)
        client-ip (:x-real-ip headers)]
    (crud/new-identifier
      (cond-> {:method   authn-method
               :username username}
              server (assoc :server server)
              client-ip (assoc :clientIP client-ip))
      p/resource-name)))

(defn validate-action-url
  [base-uri session-id]
  (codec/url-encode (str base-uri session-id "/validate")))

(defmethod p/tpl->session authn-method
  [resource {:keys [headers base-uri] :as request}]
  (let [[oidc-client-id oidc-base-url oidc-public-key] (oidc-client-info)]
    (if (and oidc-base-url oidc-client-id oidc-public-key)
      (let [session (create-session {:username "_"} headers) ;; FIXME: Remove username from required parameters.
            session (assoc session :expiry (str (tsutil/expiry-later login-request-timeout)))
            redirect-url (str oidc-base-url (format oidc-relative-url oidc-client-id (validate-action-url base-uri (:id session))))]
        [{:status 307, :headers {"Location" redirect-url}} session])
      (throw-bad-client-config))))

;; add a "validate" action (callback) to complete the GitHub authentication workflow
(defmethod p/set-session-operations authn-method
  [{:keys [id resourceURI username] :as resource} request]
  (let [href (str id "/validate")
        ops (cond-> (p/standard-session-operations resource request)
                    (= "_" username) (conj {:rel (:validate c/action-uri) :href href}))] ;; FIXME: Should just be absent! (nil? username)
    (cond-> (dissoc resource :operations)
            (seq ops) (assoc :operations ops))))

(defn response-created [id cookie]
  {:status  201
   :headers {"Location" id}
   :cookies {"com.sixsq.slipstream.cookie" cookie}})

(defn extract-session-id
  [uri]
  (second (re-matches #".*(session/[^/]+)/.*" uri)))

(defn extract-session-uuid
  [session-id]
  (second (re-matches #"session/(.+)" session-id)))

(def internal-edit (std-crud/edit-fn p/resource-name))

;; execute the "validate" callback to complete the GitHub authentication workflow
(defmethod p/validate-callback authn-method
  [resource {:keys [headers] :as request}]
  (let [[oidc-client-id oidc-base-url oidc-public-key] (oidc-client-info)]
    (if (and oidc-client-id oidc-base-url)
      (if-let [code (uh/param-value request :code)]
        (if-let [access-token (auth-oidc/get-oidc-access-token oidc-client-id oidc-base-url code "UNUSED_REDIRECT")]
          (try
            (let [claims (sign/unsign-claims access-token :oidc-public-key)
                  username (auth-oidc/login-name claims)
                  email (:email claims)]
              (if (and username email)
                (let [[matched-user _] (ex/match-external-user! :cyclone username email)]
                  (if matched-user
                    (let [session-id (extract-session-id (:uri request))
                          {:keys [server clientIP] :as current-session} (crud/retrieve-by-id session-id
                                                                                             {:user-name  "INTERNAL"
                                                                                              :user-roles [session-id]})
                          claims (cond-> (auth-internal/create-claims matched-user)
                                         session-id (update :roles #(str session-id " " %))
                                         server (assoc :server server)
                                         clientIP (assoc :clientIP clientIP))
                          cookie (cookies/claims-cookie claims)
                          expires (:expires cookie)
                          updated-session (assoc current-session
                                            :username matched-user
                                            :expiry expires)
                          {:keys [status] :as resp} (internal-edit {:user-name  "INTERNAL"
                                                                    :user-roles [session-id]
                                                                    :identity   {:current         "INTERNAL"
                                                                                 :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                                                               :roles    [session-id]}}}
                                                                    :params     {:uuid (extract-session-uuid session-id)}
                                                                    :body       updated-session})]
                      (if (not= status 200)
                        resp
                        (response-created session-id cookie)))))
                (throw-no-user-info)))
            (catch Exception _
              (throw-no-user-info)))
          (throw-no-access-token))
        (throw-missing-oidc-code))
      (throw-bad-client-config))))
