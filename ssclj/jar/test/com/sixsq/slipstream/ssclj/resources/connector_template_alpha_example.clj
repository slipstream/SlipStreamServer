(ns com.sixsq.slipstream.ssclj.resources.connector-template-alpha-example
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const cloud-service-type "alpha")

;;
;; schemas
;;

(def ConnectorTemplateAttrs
  {:alphaKey c/PosInt})

(def ConnectorTemplateAlpha
  (merge p/ConnectorTemplate
         ConnectorTemplateAttrs))

(def ConnectorTemplateAlphaRef
  (s/constrained
    (merge ConnectorTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    seq 'not-empty?))

(def ConnectorTemplateAlphaDescription
  (merge p/ConnectorTemplateDescription
         {:alphaKey {:displayName "Alpha Key"
                     :category    "general"
                     :description "example parameter"
                     :type        "int"
                     :mandatory   true
                     :readOnly    false
                     :order       1}}))
;;
;; resource
;;
(def ^:const resource
  {:cloudServiceType cloud-service-type
   :alphaKey         1001})

;;
;; description
;;
(def ^:const desc ConnectorTemplateAlphaDescription)

;;
;; initialization: register this connector template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn ConnectorTemplateAlpha))
(defmethod p/validate-subtype cloud-service-type
  [resource]
  (validate-fn resource))
