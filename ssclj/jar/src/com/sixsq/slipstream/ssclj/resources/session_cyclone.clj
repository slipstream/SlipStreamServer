(ns com.sixsq.slipstream.ssclj.resources.session-cyclone
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.spec.session]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
    [com.sixsq.slipstream.ssclj.resources.session-oidc.utils :as oidc-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-cyclone]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-cyclone :as tpl]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.cyclone :as auth-cyclone]
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

(def ^:const authn-method "cyclone")

(def ^:const login-request-timeout (* 3 60))

(def ^:const cyclone-relative-url "/auth?response_type=code&client_id=%s&redirect_uri=%s")

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
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/session-template.cyclone-create))
(defmethod p/create-validate-subtype authn-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into session resource
;;
(defmethod p/tpl->session authn-method
  [{:keys [href redirectURI] :as resource} {:keys [headers base-uri] :as request}]
  (let [[client-id base-url public-key] (oidc-utils/config-params redirectURI (u/document-id href))]
    (if (and base-url client-id public-key)
      (let [session-init (cond-> {:href href}
                                 redirectURI (assoc :redirectURI redirectURI))
            session (sutils/create-session session-init headers authn-method)
            session (assoc session :expiry (ts/format-timestamp (tsutil/expiry-later login-request-timeout)))
            redirect-url (str base-url (format cyclone-relative-url client-id (sutils/validate-action-url base-uri (:id session))))]
        [{:status 303, :headers {"Location" redirect-url}} session])
      (oidc-utils/throw-bad-client-config authn-method redirectURI))))

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
        instance (u/document-id href)
        [client-id base-url public-key] (oidc-utils/config-params redirectURI instance)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-cyclone/get-oidc-access-token client-id base-url code (sutils/validate-action-url-unencoded base-uri (or (:id resource) "unknown-id")))]
        (try
          (let [claims (sign/unsign-claims access-token public-key)
                username (auth-cyclone/login-name claims)
                email (:email claims)]
            (log/debug "cyclone access token claims for" instance ":" claims)
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
                    (log/debug "cyclone cookie token claims for" instance ":" claims)
                    (if (not= status 200)
                      resp
                      (let [cookie-tuple [(sutils/cookie-name session-id) cookie]]
                        (if redirectURI
                          (r/response-final-redirect redirectURI cookie-tuple)
                          (r/response-created session-id cookie-tuple)))))
                  (oidc-utils/throw-no-matched-user username email redirectURI)))
              (oidc-utils/throw-no-username-or-email username email redirectURI)))
          (catch Exception e
            (oidc-utils/throw-invalid-access-code (str e) redirectURI)))
        (oidc-utils/throw-no-access-token redirectURI))
      (oidc-utils/throw-missing-oidc-code redirectURI))))
