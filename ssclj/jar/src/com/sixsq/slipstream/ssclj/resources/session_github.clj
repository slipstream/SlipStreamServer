(ns com.sixsq.slipstream.ssclj.resources.session-github
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [environ.core :as environ]
    [ring.util.codec :as codec]

    [com.sixsq.slipstream.ssclj.resources.spec.session]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-github]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
    [com.sixsq.slipstream.ssclj.resources.session-template-github :as tpl]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.github :as auth-github]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.response :as r]
    [com.sixsq.slipstream.auth.utils.sign :as sg]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.utils.timestamp :as tsutil]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]))

(def ^:const authn-method "github")

(def ^:const login-request-timeout (* 3 60))

(def ^:const github-oath-endpoint "https://github.com/login/oauth/authorize?scope=user:email&client_id=%s&redirect_uri=%s")

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

(defn throw-bad-client-config [cfg-id redirectURI]
  (logu/log-error-and-throw-with-redirect 500 (str "missing or incorrect configuration (" cfg-id ") for GitHub authentication") redirectURI))

(defn throw-missing-oauth-code [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "GitHub authentication callback request does not contain required code" redirectURI))

(defn throw-no-access-token [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "unable to retrieve GitHub access code" redirectURI))

(defn throw-no-user-info [redirectURI]
  (logu/log-error-and-throw-with-redirect 400 "unable to retrieve GitHub user information" redirectURI))

(defn throw-no-matched-user [redirectURI]
  (logu/log-error-and-throw-with-redirect 403 "no matching account for GitHub user" redirectURI))

(defn github-client-info
  [redirectURI methodKey]
  (let [client-id (environ/env (keyword (str "github-client-id-" methodKey)))
        client-secret (environ/env (keyword (str "github-client-secret-" methodKey)))]
    (if (and client-id client-secret)
      [client-id client-secret]
      (throw-bad-client-config redirectURI))))

(defn config-params
  [redirectURI methodKey]
  (let [cfg-id (str "configuration/session-github-" methodKey)
        opts {:user-name "INTERNAL" :user-roles ["ADMIN"]}] ;; FIXME: works around authn at DB interface level
    (try
      (let [{:keys [clientID clientSecret]} (crud/retrieve-by-id cfg-id opts)]
        (if (and clientID clientSecret)
          [clientID clientSecret]
          (throw-bad-client-config cfg-id redirectURI)))
      (catch Exception _
        (throw-bad-client-config cfg-id redirectURI)))))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))


(def create-validate-fn (u/create-spec-validation-fn :cimi/session-template.github-create))
(defmethod p/create-validate-subtype authn-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into session resource
;;

;; creates a temporary session and redirects to GitHub to start authentication workflow
(defmethod p/tpl->session authn-method
  [{:keys [href redirectURI] :as resource} {:keys [headers base-uri] :as request}]
  (let [[client-id client-secret] (config-params redirectURI (u/document-id href))]
    (if (and client-id client-secret)
      (let [session-init (cond-> {:href href}
                                 redirectURI (assoc :redirectURI redirectURI))
            session (sutils/create-session session-init headers authn-method)
            session (assoc session :expiry (ts/format-timestamp (tsutil/expiry-later login-request-timeout)))
            redirect-url (format github-oath-endpoint client-id (sutils/validate-action-url base-uri (:id session)))]
        [{:status 303, :headers {"Location" redirect-url}} session])
      (throw-bad-client-config authn-method redirectURI))))

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
  [resource {:keys [headers uri redirectURI] :as request}]
  (let [session-id (sutils/extract-session-id uri)
        {:keys [server clientIP redirectURI] {:keys [href]} :sessionTemplate :as current-session} (sutils/retrieve-session-by-id session-id)
        methodKey (u/document-id href)
        [client-id client-secret] (config-params redirectURI methodKey)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-github/get-github-access-token client-id client-secret code)]
        (if-let [{:keys [user email] :as user-info} (auth-github/get-github-user-info access-token)]
          (do
            (log/debug "github user info for" methodKey ":" user-info)
            (let [external-login (auth-github/sanitized-login user-info)
                  external-email (auth-github/retrieve-email user-info access-token)
                  [matched-user _] (ex/match-external-user! :github external-login external-email)]
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
                  (log/debug "github cookie token claims for" methodKey ":" claims)
                  (if (not= status 200)
                    resp
                    (let [cookie-tuple [(sutils/cookie-name session-id) cookie]]
                      (if redirectURI
                        (r/response-final-redirect redirectURI cookie-tuple)
                        (r/response-created session-id cookie-tuple)))))
                (throw-no-matched-user redirectURI))))
          (throw-no-user-info redirectURI))
        (throw-no-access-token redirectURI))
      (throw-missing-oauth-code redirectURI))))
