(ns com.sixsq.slipstream.ssclj.resources.session-github
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.spec.session]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-github]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-github :as tpl]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.auth.utils.sign :as sg]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.utils.timestamp :as tsutil]
    [environ.core :as environ]))

(def ^:const authn-method "github")

(def ^:const login-request-timeout (* 3 60))

(def ^:const github-oath-endpoint "https://github.com/login/oauth/authorize?client_id=%s&scope=user:email")

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


(def create-validate-fn (u/create-spec-validation-fn :cimi/session-template.github-create))
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
      (cond-> {:method authn-method
               :username    username}
              server (assoc :server server)
              client-ip (assoc :clientIP client-ip))
      p/resource-name)))

(defmethod p/tpl->session authn-method
  [resource {:keys [headers] :as request}]
  (if-let [client-id (environ/env :github-client-id)]
    (let [redirect-url (format github-oath-endpoint client-id)
          session (create-session {:username "_"} headers)  ;; FIXME: Remove username from required parameters.
          session (assoc session :expiry (str (tsutil/expiry-later login-request-timeout)))]
      [{:status 307, :headers {"Location" redirect-url}} session])
    (let [msg "missing client ID (:github-client-id) for GitHub authentication"]
      (throw (ex-info msg {:status 500, :message msg})))))
