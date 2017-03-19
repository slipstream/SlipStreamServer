(ns com.sixsq.slipstream.ssclj.resources.session-template-github
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "github")

;;
;; schemas
;;

(def SessionTemplateAttrs
  p/SessionTemplateAttrs)

(def SessionTemplate
  (merge p/SessionTemplate
         SessionTemplateAttrs))

(def SessionTemplateRef
  (s/constrained
    (merge SessionTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    seq 'not-empty?))

;;
;; resource
;;
(def ^:const resource
  {:authnMethod authn-method
   :name        "GitHub"
   :description "Authentication via GitHub OAuth"
   })

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
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn SessionTemplate))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
