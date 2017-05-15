(ns com.sixsq.slipstream.ssclj.resources.session-oidc
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.spec.session]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
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
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]))

(def ^:const authn-method "oidc")

(def ^:const login-request-timeout (* 3 60))

(def ^:const oidc-relative-url "/auth?response_type=code&client_id=%s&redirect_uri=%s")

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

(defn throw-bad-client-config []
  (log-util/log-and-throw 500 "missing client ID, base URL, or public key (:oidc-client-id, :oidc-base-url, :oidc-public-key) for OIDC authentication"))

(defn throw-missing-oidc-code []
  (log-util/log-and-throw 400 "OIDC authentication callback request does not contain required code"))

(defn throw-no-access-token []
  (log-util/log-and-throw 400 "unable to retrieve OIDC access token"))

(defn throw-no-user-info []
  (log-util/log-and-throw 400 "unable to retrieve or decrypt OIDC user information"))

(defn oidc-client-info
  []
  (let [client-id (environ/env :oidc-client-id)
        base-url (environ/env :oidc-base-url)
        public-key (environ/env :oidc-public-key)]
    (if (and client-id base-url public-key)
      [client-id base-url public-key]
      (throw-bad-client-config))))

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
(defmethod p/tpl->session authn-method
  [resource {:keys [headers base-uri] :as request}]
  (let [[oidc-client-id oidc-base-url oidc-public-key] (oidc-client-info)]
    (if (and oidc-base-url oidc-client-id oidc-public-key)
      (let [session (sutils/create-session {:username "_"} headers authn-method) ;; FIXME: Remove username from required parameters.
            session (assoc session :expiry (ts/format-timestamp (tsutil/expiry-later login-request-timeout)))
            redirect-url (str oidc-base-url (format oidc-relative-url oidc-client-id (sutils/validate-action-url base-uri (:id session))))]
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

;; execute the "validate" callback to complete the GitHub authentication workflow
(defmethod p/validate-callback authn-method
  [resource {:keys [headers base-uri] :as request}]
  (let [[oidc-client-id oidc-base-url oidc-public-key] (oidc-client-info)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-oidc/get-oidc-access-token oidc-client-id oidc-base-url code (sutils/validate-action-url base-uri (or (:id resource) "unknown-id")))]
        (try
          (let [claims (sign/unsign-claims access-token :oidc-public-key)
                username (auth-oidc/login-name claims)
                email (:email claims)]
            (if (and username email)
              (let [[matched-user _] (ex/match-external-user! :cyclone username email)]
                (if matched-user
                  (let [session-id (sutils/extract-session-id (:uri request))
                        {:keys [server clientIP] :as current-session} (sutils/retrieve-session-by-id session-id)
                        claims (cond-> (auth-internal/create-claims matched-user)
                                       session-id (update :roles #(str session-id " " %))
                                       server (assoc :server server)
                                       clientIP (assoc :clientIP clientIP))
                        cookie (cookies/claims-cookie claims)
                        expires (:expires cookie)
                        updated-session (assoc current-session
                                          :username matched-user
                                          :expiry expires)
                        {:keys [status] :as resp} (sutils/update-session session-id updated-session)]
                    (if (not= status 200)
                      resp
                      (u/response-created session-id [(sutils/cookie-name session-id) cookie])))))
              (throw-no-user-info)))
          (catch Exception _
            (throw-no-user-info)))
        (throw-no-access-token))
      (throw-missing-oidc-code))))
