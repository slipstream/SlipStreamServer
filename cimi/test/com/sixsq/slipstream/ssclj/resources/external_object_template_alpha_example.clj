(ns com.sixsq.slipstream.ssclj.resources.external-object-template-alpha-example
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [com.sixsq.slipstream.ssclj.resources.external-object-template :as eo]
            [com.sixsq.slipstream.ssclj.resources.spec.external-object-template :as eot]
            [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [com.sixsq.slipstream.ssclj.util.spec :as su]))


(def ^:const objectType "alpha")

;;
;; schemas
;;

(s/def :cimi.external-object-template.alpha/alphaKey pos-int?)

;; Defines the contents of the alpha ExternalObjectTemplate resource itself.
(s/def :cimi/external-object-template.alpha
  (su/only-keys-maps eot/resource-keys-spec
                     {:req-un [:cimi.external-object-template.alpha/alphaKey]}))

;; Defines the contents of the alpha template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :externalObjectTemplate here.
(s/def :cimi.external-object-template.alpha/externalObjectTemplate
  (su/only-keys-maps eot/template-keys-spec
                     {:opt-un [:cimi.external-object-template.alpha/alphaKey]}))

(s/def :cimi/external-object-template.alpha-create
  (su/only-keys-maps eot/create-keys-spec
                     {:opt-un [:cimi.external-object-template.alpha/externalObjectTemplate]}))

(def ExternalObjectTemplateAlphaDescription
  (merge eo/ExternalObjectTemplateDescription
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
  {:objectType objectType
   :alphaKey         1001})

;;
;; description
;;
(def ^:const desc ExternalObjectTemplateAlphaDescription)

;;
;; initialization: register this connector template
;;
(defn initialize
  []
  (eo/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object-template.alpha))
(defmethod eo/validate-subtype objectType
  [resource]
  (validate-fn resource))





