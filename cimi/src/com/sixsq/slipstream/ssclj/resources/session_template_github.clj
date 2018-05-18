(ns com.sixsq.slipstream.ssclj.resources.session-template-github
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-github]))

(def ^:const authn-method "github")

;;
;; description
;;
(def ^:const desc
  p/SessionTemplateDescription)

;;
;; initialization: register this Session template
;;
(defn initialize
  []
  (p/register authn-method desc)
  (std-crud/initialize p/resource-url :cimi/session-template.github))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.github))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
