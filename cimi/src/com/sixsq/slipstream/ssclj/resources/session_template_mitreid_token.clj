(ns com.sixsq.slipstream.ssclj.resources.session-template-mitreid-token
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-mitreid-token :as session-tpl]))


(def ^:const authn-method "mitreid-token")


(def default-template {:method      authn-method
                       :instance    authn-method
                       :name        "OIDC Token"
                       :description "Direct Authentication with OIDC Token"
                       :token       "token"
                       :acl         p/resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription
         {:token {:displayName "OIDC Token"
                  :category    "general"
                  :description "OIDC Token"
                  :type        "string"
                  :mandatory   true
                  :readOnly    false
                  :order       20}}))


;;
;; initialization: register this Session template
;;
(defn initialize
  []
  (p/register authn-method desc)
  (std-crud/initialize p/resource-url ::session-tpl/oidc-token))


;;
;; multimethods for validation
;;
(def validate-fn (u/create-spec-validation-fn ::session-tpl/oidc-token))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
