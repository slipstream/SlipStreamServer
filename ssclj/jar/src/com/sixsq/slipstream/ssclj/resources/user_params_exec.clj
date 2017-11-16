(ns com.sixsq.slipstream.ssclj.resources.user-params-exec
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.user-params-template-exec]
    [com.sixsq.slipstream.ssclj.resources.user-params :as p]
    [com.sixsq.slipstream.ssclj.resources.user-params-template-exec :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/user-params-template.exec))
(defmethod p/validate-subtype tpl/params-type
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/user-params-template.exec-create))
(defmethod p/create-validate-subtype tpl/params-type
  [resource]
  (create-validate-fn resource))

;;
;; transform template into user params esource

(defmethod p/tpl->user-params tpl/params-type
  [resource request]
  (-> resource
      (assoc :resourceURI p/resource-uri)))
