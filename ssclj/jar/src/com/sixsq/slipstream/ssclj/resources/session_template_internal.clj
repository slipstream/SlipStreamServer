(ns com.sixsq.slipstream.ssclj.resources.session-template-internal
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "internal")

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
                     :order       3}
          :password {:displayName "Password"
                     :category    "general"
                     :description "password"
                     :type        "password"
                     :mandatory   true
                     :readOnly    false
                     :order       4}}))

;;
;; initialization: register this Session template
;;
(defn initialize
  []
  (p/register authn-method desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.internal))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
