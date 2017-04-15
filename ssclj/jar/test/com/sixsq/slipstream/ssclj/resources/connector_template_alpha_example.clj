(ns com.sixsq.slipstream.ssclj.resources.connector-template-alpha-example
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.connector-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(def ^:const cloud-service-type "alpha")

;;
;; schemas
;;

(s/def :cimi.connector-template.alpha/alphaKey pos-int?)

(def keys-spec (su/merge-keys-specs
                 [ps/connector-template-attrs-keys
                  {:req-un [:cimi.connector-template.alpha/alphaKey]}]))

(s/def :cimi/connector-template-alpha (su/only-keys-maps keys-spec))

(def ref-keys-spec (su/merge-keys-specs
                     [keys-spec
                      {:opt-un [:cimi.connector-template/href]}]))

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

(def validate-fn (u/create-spec-validation-fn :cimi/connector-template-alpha))
(defmethod p/validate-subtype cloud-service-type
  [resource]
  (validate-fn resource))
