(ns com.sixsq.slipstream.ssclj.resources.session-internal
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.spec.session]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-internal :as tpl]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.auth.utils.sign :as sg]
    [com.sixsq.slipstream.auth.cookies :as cookies]))

(def ^:const authn-method "internal")

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

(def create-validate-fn (u/create-spec-validation-fn :cimi/session-template.internal-create))
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

;; FIXME: Multiple session cookies should be permitted, eventually.
;; FIXME: For backward compatibility use the standard name of the cookie.
(defn cookie-name [{:keys [id]}]
  #_(str "slipstream." (str/replace id "/" "."))
  "com.sixsq.slipstream.cookie")

(defn create-claims [credentials headers session]
  (let [server (:slipstream-ssl-server-hostname headers)]
    (cond-> (auth-internal/create-claims credentials)
            server (assoc :server server)
            session (assoc :session session))))

(defmethod p/tpl->session authn-method
  [resource {:keys [headers] :as request}]
  (let [credentials (select-keys resource #{:username :password})]
    (if (auth-internal/valid? credentials)
      (let [session (create-session credentials headers)
            claims (create-claims credentials headers session)
            cookie (cookies/claims-cookie claims)
            expires (:expires cookie)
            session (assoc session :expiry expires)]
        [{:cookies {(cookie-name session) cookie}} session])
      (throw (u/ex-unauthorized (:username credentials))))))
