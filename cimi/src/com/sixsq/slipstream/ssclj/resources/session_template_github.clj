(ns com.sixsq.slipstream.ssclj.resources.session-template-github
  "docs to make utility happy"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-github :as session-tpl]))

(def resource-url "github")
(def resource-name "GitHub")

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
  (std-crud/initialize p/resource-url ::session-tpl/github))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::session-tpl/github))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
