(ns com.sixsq.slipstream.ssclj.resources.external-object-template-report
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eo]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template :as eot]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [clojure.spec.alpha :as s])
  )

(def ^:const objectType "report")




;; Defines the contents of the report ExternalObjectTemplate resource itself.
(s/def :cimi/external-object-template.report
  (su/only-keys-maps eot/resource-keys-spec))

;; Defines the contents of the report template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :externalObjectTemplate here.
(s/def :cimi.external-object-template.report/externalObjectTemplate
  (su/only-keys-maps eot/template-keys-spec))

(s/def :cimi/external-object-template.report-create
  (su/only-keys-maps eot/create-keys-spec
                     {:opt-un [:cimi.external-object-template.report/externalObjectTemplate]}))


(def ExternalObjectTemplateReportDescription eo/ExternalObjectTemplateDescription)

;;
;; resource
;;
(def ^:const resource
  {:objectType objectType
   :state "new"})


;;
;; description
;;
(def ^:const desc ExternalObjectTemplateReportDescription)

;;
;; initialization: register this external object report template
;;
(defn initialize
  []
  (eo/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object-template.report))
(defmethod eo/validate-subtype objectType
  [resource]
  (validate-fn resource))
