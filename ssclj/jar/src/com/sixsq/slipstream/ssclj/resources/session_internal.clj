(ns com.sixsq.slipstream.ssclj.resources.session-internal
  (:require
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-internal :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "internal")

;;
;; schemas
;;

(def SessionAttrs
  tpl/SessionTemplateAttrs)

(def Session
  (merge p/Session
         SessionAttrs))

(def SessionCreate
  (merge c/CreateAttrs
         {:sessionTemplate tpl/SessionTemplateRef}))

(def SessionDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc SessionDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn Session))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn SessionCreate))
(defmethod p/create-validate-subtype authn-method
  [resource]
  (create-validate-fn resource))
