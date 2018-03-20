(ns com.sixsq.slipstream.ssclj.resources.external-object-template-generic
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eo]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-generic]))

(def ^:const objectType "generic")

(def ExternalObjectTemplateGenericDescription
  (merge eo/ExternalObjectTemplateDescription
         {:objectName {:displayName "Object name of the object"
                       :category    "general"
                       :description "Name of the object"
                       :type        "string"
                       :mandatory   false
                       :readOnly    true
                       :order       20}
          :bucketName {:displayName "Bucket name"
                       :category    "general"
                       :description "Name of the bucket to store the object to"
                       :type        "string"
                       :mandatory   true
                       :readOnly    true
                       :order       21}}))


;;
;; resource
;;
(def ^:const resource
  {:objectType objectType
   :state      "new"})


;;
;; description
;;
(def ^:const desc ExternalObjectTemplateGenericDescription)


;;
;; initialization: register this external object generic template
;;
(defn initialize
  []
  (eo/register resource desc))


;;
;; multimethods for validation
;;
(def validate-fn (u/create-spec-validation-fn :cimi/external-object-template.generic))


(defmethod eo/validate-subtype objectType
  [resource]
  (validate-fn resource))
