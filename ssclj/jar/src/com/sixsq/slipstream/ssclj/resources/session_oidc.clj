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
    [com.sixsq.slipstream.ssclj.util.log :as logu]
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
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.ssclj.util.response :as r]
    [clojure.tools.logging :as log]))

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

(defn throw-bad-client-config [redirectURI]
  (logu/log-error-and-throw-with-redirect 500 "missing client ID, base URL, or public key (:oidc-client-id, :oidc-base-url, :oidc-public-key) for OIDC authentication" redirectURI))

(defn throw-missing-oidc-code [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "OIDC authentication callback request does not contain required code" redirectURI))

(defn throw-no-access-token [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "unable to retrieve OIDC access token" redirectURI))

(defn throw-no-username-or-email [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "OIDC token is missing name/preferred_name (" username ") or email (" email ")") redirectURI))

(defn throw-no-matched-user [username email redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "Unable to match account to name/preferred_name (" username ") or email (" email ")") redirectURI))

(defn throw-invalid-access-code [msg redirectURI]
  (logu/log-error-and-throw-with-redirect 400 (str "error when processing OIDC access token: " msg) redirectURI))

(defn oidc-client-info
  [redirectURI methodKey]
  (let [client-id (environ/env (keyword (str "oidc-client-id-" methodKey)))
        base-url (environ/env (keyword (str "oidc-base-url-" methodKey)))
        public-key (environ/env (keyword (str "oidc-public-key-" methodKey)))]
    (if (and client-id base-url public-key)
      [client-id base-url public-key]
      (throw-bad-client-config redirectURI))))

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
  [{:keys [href redirectURI] :as resource} {:keys [headers base-uri] :as request}]
  (let [[oidc-client-id oidc-base-url oidc-public-key] (oidc-client-info redirectURI (u/document-id href))]
    (if (and oidc-base-url oidc-client-id oidc-public-key)
      (let [session-init (cond-> {:href href}
                                 redirectURI (assoc :redirectURI redirectURI))
            session (sutils/create-session session-init headers authn-method)
            session (assoc session :expiry (ts/format-timestamp (tsutil/expiry-later login-request-timeout)))
            redirect-url (str oidc-base-url (format oidc-relative-url oidc-client-id (sutils/validate-action-url base-uri (:id session))))]
        [{:status 303, :headers {"Location" redirect-url}} session])
      (throw-bad-client-config redirectURI))))

;; add a "validate" action (callback) to complete the GitHub authentication workflow
(defmethod p/set-session-operations authn-method
  [{:keys [id resourceURI username] :as resource} request]
  (let [href (str id "/validate")
        ops (cond-> (p/standard-session-operations resource request)
                    (nil? username) (conj {:rel (:validate c/action-uri) :href href}))]
    (cond-> (dissoc resource :operations)
            (seq ops) (assoc :operations ops))))

;; execute the "validate" callback to complete the GitHub authentication workflow
(defmethod p/validate-callback authn-method
  [resource {:keys [headers base-uri uri] :as request}]
  (let [session-id (sutils/extract-session-id uri)
        {:keys [server clientIP redirectURI] {:keys [href]} :sessionTemplate :as current-session} (sutils/retrieve-session-by-id session-id)
        methodKey (u/document-id href)
        [oidc-client-id oidc-base-url oidc-public-key] (oidc-client-info redirectURI methodKey)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-oidc/get-oidc-access-token oidc-client-id oidc-base-url code (sutils/validate-action-url-unencoded base-uri (or (:id resource) "unknown-id")))]
        (try
          (let [claims (sign/unsign-claims access-token (keyword (str "oidc-public-key-" methodKey)))
                username (auth-oidc/login-name claims)
                email (:email claims)]
            (log/debug "oidc access token claims for" methodKey ":" claims)
            (if (or username email)
              (let [[matched-user _] (ex/match-external-user! :cyclone username email)]
                (if matched-user
                  (let [claims (cond-> (auth-internal/create-claims matched-user)
                                       session-id (assoc :session session-id)
                                       session-id (update :roles #(str session-id " " %))
                                       server (assoc :server server)
                                       clientIP (assoc :clientIP clientIP))
                        cookie (cookies/claims-cookie claims)
                        expires (:expires cookie)
                        updated-session (assoc current-session
                                          :username matched-user
                                          :expiry expires)
                        {:keys [status] :as resp} (sutils/update-session session-id updated-session)]
                    (log/debug "oidc cookie token claims for" methodKey ":" claims)
                    (if (not= status 200)
                      resp
                      (let [cookie-tuple [(sutils/cookie-name session-id) cookie]]
                        (if redirectURI
                          (r/response-final-redirect redirectURI cookie-tuple)
                          (r/response-created session-id cookie-tuple)))))
                  (throw-no-matched-user username email redirectURI)))
              (throw-no-username-or-email username email redirectURI)))
          (catch Exception e
            (throw-invalid-access-code (str e) redirectURI)))
        (throw-no-access-token redirectURI))
      (throw-missing-oidc-code redirectURI))))
