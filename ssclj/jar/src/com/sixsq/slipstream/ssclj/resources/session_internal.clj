(ns com.sixsq.slipstream.ssclj.resources.session-internal
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.spec.session]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
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

(defn create-claims [{:keys [username] :as credentials} headers session]
  (let [server (:slipstream-ssl-server-hostname headers)]
    (cond-> (auth-internal/create-claims username)
            server (assoc :server server)
            session (assoc :session session))))

(defmethod p/tpl->session authn-method
  [resource {:keys [headers] :as request}]
  (let [credentials (select-keys resource #{:username :password})]
    (if (auth-internal/valid? credentials)
      (let [session (sutils/create-session credentials headers authn-method)
            claims (create-claims credentials headers session)
            cookie (cookies/claims-cookie claims)
            expires (:expires cookie)
            session (assoc session :expiry expires)]
        [{:cookies {(sutils/cookie-name (:id session)) cookie}} session])
      (throw (u/ex-unauthorized (:username credentials))))))
