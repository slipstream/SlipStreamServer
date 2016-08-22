(ns com.sixsq.slipstream.ssclj.resources.connector-alpha-example
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.connector :as p]
    [com.sixsq.slipstream.ssclj.resources.connector-template-alpha-example :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const cloud-service-type "alpha")

;;
;; schemas
;;

(def ConnectorAttrs
  {:alphaKey c/PosInt})

(def ConnectorAlpha
  (merge p/Connector
         ConnectorAttrs))

(def ConnectorAlphaCreate
  (merge c/CreateAttrs
         {:connectorTemplate tpl/ConnectorTemplateAlphaRef}))

(def ConnectorAlphaDescription
  tpl/ConnectorTemplateAlphaDescription)

;;
;; description
;;
(def ^:const desc ConnectorAlphaDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn ConnectorAlpha))
(defmethod p/validate-subtype cloud-service-type
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn ConnectorAlphaCreate))
(defmethod p/create-validate-subtype cloud-service-type
  [resource]
  (create-validate-fn resource))
