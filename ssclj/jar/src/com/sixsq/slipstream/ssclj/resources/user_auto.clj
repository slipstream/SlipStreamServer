(ns com.sixsq.slipstream.ssclj.resources.user-auto
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-auto]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-auto :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/user-template.auto))
(defmethod p/validate-subtype tpl/registration-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/user-template.auto-create))
(defmethod p/create-validate-subtype tpl/registration-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into user resource
;; strips method attribute and updates the resource URI
;;

(defmethod p/tpl->user tpl/registration-method
  [resource request]
  (-> resource
      (assoc :resourceURI p/resource-uri)
      (assoc :isSuperUser false)))
