(ns com.sixsq.slipstream.ssclj.resources.session-template-api-key
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-api-key]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]))

(def ^:const authn-method "api-key")

(def default-template {:method      authn-method
                       :instance    authn-method
                       :name        "API Key"
                       :description "Authentication with API Key and Secret"
                       :username    "key"
                       :password    "secret"
                       :acl         p/resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription
         {:key    {:displayName "Key"
                   :category    "general"
                   :description "API key"
                   :type        "string"
                   :mandatory   true
                   :readOnly    false
                   :order       20}
          :secret {:displayName "Secret"
                   :category    "general"
                   :description "secret associated with API key"
                   :type        "password"
                   :mandatory   true
                   :readOnly    false
                   :order       21}}))

;;
;; initialization: register this Session template
;;
(defn initialize
  []
  (p/register authn-method desc)
  (std-crud/initialize p/resource-url :cimi/session-template.api-key))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.api-key))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
