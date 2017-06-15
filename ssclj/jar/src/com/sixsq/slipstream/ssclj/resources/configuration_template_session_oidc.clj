(ns com.sixsq.slipstream.ssclj.resources.configuration-template-session-oidc
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-oidc]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const service "session-oidc")

;;
;; resource
;;
(def ^:const resource
  {:service     service
   :name        "OIDC Authentication Configuration"
   :description "OpenID Connect Authentication Configuration"
   :instance    "authn-name"
   :clientID    "server-assigned-client-id"
   :baseURL     "https://keycloak.example.org/auth/realms/myorg/protocol/openid-connect"
   :publicKey   "ABCDEF..."})


;;
;; description
;;
(def ^:const desc
  (merge p/ConfigurationTemplateDescription
         {:clientID {:displayName "Client ID"
                      :type        "string"
                      :description "client identifier assigned by the OIDC server"
                      :mandatory   true
                      :readOnly    false
                      :order       1}
          :baseURL {:displayName "Base URL"
                      :type        "string"
                      :description "server's endpoint URL for the OIDC protocol"
                      :mandatory   true
                      :readOnly    false
                      :order       2}
          :publicKey {:displayName "Public Key"
                      :type        "string"
                      :description "public key to verify signed tokens from the OIDC server"
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

(def validate-fn (u/create-spec-validation-fn :cimi/configuration-template.session-oidc))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))
