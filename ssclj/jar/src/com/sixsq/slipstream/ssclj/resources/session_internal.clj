(ns com.sixsq.slipstream.ssclj.resources.session-internal
  (:require
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-internal :as tpl]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.auth.sign :as sg]))

(def ^:const authn-method "internal")

;;
;; schemas
;;

(def SessionAttrs
  tpl/SessionTemplateAttrs)

(def Session
  (merge p/Session
         SessionAttrs))

(def SessionCreate
  (merge c/CreateAttrs
         {:sessionTemplate tpl/SessionTemplateRef}))

(def SessionDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc SessionDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn Session))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn SessionCreate))
(defmethod p/create-validate-subtype authn-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into session resource
;;
(defn create-session [{:keys [username]} headers]
  (crud/new-identifier
    {:authnMethod authn-method
     :username    username
     :virtualHost (:slipstream-ssl-server-hostname headers)
     :clientIP    (:x-real-ip headers)
     :expiry      (sg/expiry-timestamp)}
    p/resource-name))

(defn cookie-header [uuid token]
  (let [cookie-name (str "slipstream." uuid)]
    {:cookies {cookie-name {:value token, :path "/"}}}))

(defn extract-uuid [{:keys [id]}]
  (second (s/split id #"/")))

(defmethod p/tpl->session authn-method
  [resource request]
  (let [headers (:headers request)
        credentials {:username (:username resource)
                     :password (:password resource)}
        [ok? token] (auth-internal/create-token credentials)]
    (when ok?
      (let [session (create-session credentials headers)]
        [(cookie-header (extract-uuid session) token) session]))))
