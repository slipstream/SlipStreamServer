(ns com.sixsq.slipstream.ssclj.resources.configuration-template-session-oidc
  (:require
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const service "authentication")

;;
;; resource
;;
(def ^:const resource
  {:service   service
   :methodKey "Value of methodKey for associated SessionTemplate resource"
   :clientID  "Client ID"
   :baseURL   "Base URL for contacting OIDC service"
   :publicKey "OIDC service's public key for token validation"})

;;
;; description
;;
(def ^:const desc
  (merge p/ConfigurationTemplateDescription
         {:methodKey {:displayName "Method Key"
                      :type        "string"
                      :description "Value of methodKey for associated SessionTemplate resource"
                      :mandatory   true
                      :readOnly    false
                      :order       0}
          :clientID  {:displayName "Client ID"
                      :type        "string"
                      :description "Client ID assigned by the OIDC server"
                      :mandatory   true
                      :readOnly    false
                      :order       1}
          :baseURL   {:displayName "Base URL"
                      :type        "string"
                      :description "Base URL for contacting the OIDC service"
                      :mandatory   true
                      :readOnly    false
                      :order       2}
          :publicKey {:displayName "Public Key"
                      :type        "string"
                      :description "OIDC service's public key for token validation"
                      :mandatory   true
                      :readOnly    false
                      :order       3}}))

;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))
