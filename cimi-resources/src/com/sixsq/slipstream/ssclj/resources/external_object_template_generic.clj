(ns com.sixsq.slipstream.ssclj.resources.external-object-template-generic
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-generic]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-generic]))

(def ^:const objectType "generic")

(def ExternalObjectTemplateGenericDescription
  (merge eot/ExternalObjectTemplateDescription
         {:objectStoreCred {:displayName "Object Store credentials."
                            :category    "general"
                            :description "Link to Object Store credential."
                            :type        "href"
                            :mandatory   true
                            :readOnly    true
                            :order       23}
          :bucketName      {:displayName "Object Store bucket name"
                            :category    "general"
                            :description "Name of the bucket to store the object to"
                            :type        "string"
                            :mandatory   true
                            :readOnly    true
                            :order       21}
          :objectName      {:displayName "Object name"
                            :category    "general"
                            :description "Name of the object to store (/can/be/hierarchical)."
                            :type        "string"
                            :mandatory   true
                            :readOnly    true
                            :order       22}}))

;;
;; resource
;;
(def ^:const resource
  {:objectType      objectType
   :contentType     "content/type"
   :objectStoreCred {:href "credential/cloud-cred"}
   :bucketName      "bucket-name"
   :objectName      "object/name"})


;;
;; description
;;
(def ^:const desc ExternalObjectTemplateGenericDescription)


;;
;; initialization: register this external object generic template
;;
(defn initialize
  []
  (eot/register resource desc))


;;
;; multimethods for validation
;;
(def validate-fn (u/create-spec-validation-fn :cimi/external-object.generic))
(defmethod eo/validate-subtype objectType
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/external-object-template.generic-create))
(defmethod eo/create-validate-subtype objectType
  [resource]
  (create-validate-fn resource))

(def validate-fn (u/create-spec-validation-fn :cimi.external-object-template.generic/externalObjectTemplate))
(defmethod eot/validate-subtype-template objectType
  [resource]
  (validate-fn resource))