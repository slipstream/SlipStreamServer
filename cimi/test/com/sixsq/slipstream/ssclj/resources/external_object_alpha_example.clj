(ns com.sixsq.slipstream.ssclj.resources.external-object-alpha-example
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-alpha-example :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    ))

(def ^:const objectType "alpha")

;; Trivial example has the same schema as the template.  A real
;; resource may have different schemas for the template and resource.
(s/def :cimi/external-object.alpha :cimi/external-object-template.alpha)

(def ExternalObjectAlphaDescription
  tpl/ExternalObjectTemplateAlphaDescription)

;;
;; description
;;
(def ^:const desc ExternalObjectAlphaDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object.alpha))
(defmethod eo/validate-subtype objectType
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/external-object-template.alpha-create))
(defmethod eo/create-validate-subtype objectType
  [resource]
  (create-validate-fn resource))
