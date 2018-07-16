(ns com.sixsq.slipstream.ssclj.resources.configuration-template-session-mitreid-token
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-mitreid-token :as ct-oidc-token]))


(def ^:const service "session-oidc-token")


;;
;; resource
;;
(def ^:const resource
  {:service       service
   :name          "OIDC Token Authentication Configuration"
   :description   "OpenID Connect Token Authentication Configuration"
   :instance      "authn-name"
   :publicKey     "ABCDEF..."
   :authzClientIP "127.0.0.1"
   :authnMethod   "other-oidc"})


;;
;; description
;;
(def ^:const desc
  (merge p/ConfigurationTemplateDescription
         {:publicKey     {:displayName "Public Key"
                          :type        "string"
                          :description "public key to verify signed tokens from the OIDC server"
                          :mandatory   true
                          :readOnly    false
                          :order       22}
          :authzClientIP {:displayName "Authz. Client IP"
                          :type        "string"
                          :description "authorized client IP address from which OIDC token authentication is allowed"
                          :mandatory   true
                          :readOnly    false
                          :order       23}
          :authnMethod   {:displayName "Authn. Method"
                          :type        "string"
                          :description "authentication method for user matching"
                          :mandatory   true
                          :readOnly    false
                          :order       24}}))


;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (p/register resource desc))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::ct-oidc-token/session-mitreid-token))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))
