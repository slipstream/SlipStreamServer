(ns com.sixsq.slipstream.ssclj.resources.external-object-generic
  (:require
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-generic :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-generic]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]))


(def ExternalObjectReportDescription
  tpl/ExternalObjectTemplateGenericDescription)

;;
;; description
;;
(def ^:const desc ExternalObjectReportDescription)

;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object-template.generic))
(defmethod eo/validate-subtype tpl/objectType
  [resource]
  (validate-fn resource))


(def create-validate-fn (u/create-spec-validation-fn :cimi/external-object-template.generic-create))
(defmethod eo/create-validate-subtype tpl/objectType
  [resource]
  (create-validate-fn resource))


(defmethod eo/tpl->externalObject tpl/objectType
  [resource]
  (if-not (:objectName resource)
    (assoc resource :objectName (cu/random-uuid))
    resource))
