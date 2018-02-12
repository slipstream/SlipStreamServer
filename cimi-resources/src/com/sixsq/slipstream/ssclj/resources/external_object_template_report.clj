(ns com.sixsq.slipstream.ssclj.resources.external-object-template-report
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object-template :as eo]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report]))

(def ^:const objectType "report")


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
