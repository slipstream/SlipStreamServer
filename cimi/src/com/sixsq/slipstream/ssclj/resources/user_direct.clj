(ns com.sixsq.slipstream.ssclj.resources.user-direct
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-direct]
    [com.sixsq.slipstream.ssclj.resources.spec.user-direct]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-direct :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; validate the create resource
;;
(def validate-fn (u/create-spec-validation-fn :cimi/user-direct))
(defmethod p/validate-subtype tpl/registration-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/user-template.direct-create))
(defmethod p/create-validate-subtype tpl/registration-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into user resource
;; just strips method attribute and updates the resource URI
;;
(defmethod p/tpl->user tpl/registration-method
  [resource request]
  (let [res (assoc resource :resourceURI p/resource-uri)]
    (if (contains? res :isSuperUser)
      res
      (assoc res :isSuperUser false))))

