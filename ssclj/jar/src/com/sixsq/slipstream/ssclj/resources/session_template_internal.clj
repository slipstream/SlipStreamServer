(ns com.sixsq.slipstream.ssclj.resources.session-template-internal
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "internal")

;;
;; schemas
;;

(def SessionTemplateAttrs
  (merge p/SessionTemplateAttrs
         {:username c/NonBlankString
          :password c/NonBlankString
          }))

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
  {:method      authn-method
   :name        "Internal"
   :description "Internal Authentication via Username/Password"
   :username    "username"
   :password    "password"
   })

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription
         {:username {:displayName "Username"
                     :category    "general"
                     :description "username"
                     :type        "string"
                     :mandatory   true
                     :readOnly    false
                     :order       1}
          :password {:displayName "Password"
                     :category    "general"
                     :description "password"
                     :type        "string"
                     :mandatory   true
                     :readOnly    false
                     :order       2}}))

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
