(ns com.sixsq.slipstream.ssclj.resources.configuration-template-session-mitreid
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-mitreid :as ct-mitreid]))

(def ^:const service "session-mitreid")

;;
;; resource
;;
(def ^:const resource
  {:service     service
   :name        "MITREid Authentication Configuration"
   :description "MITREid OpenID Connect Authentication Configuration"
   :instance    "authn-name"
   :clientID    "server-assigned-client-id"
   :publicKey   "ABCDEF..."})


;;
;; description
;;
(def ^:const desc
  (merge p/ConfigurationTemplateDescription
         {:clientID     {:displayName "Client ID"
                         :type        "string"
                         :description "client identifier assigned by the MITREid server"
                         :mandatory   true
                         :readOnly    false
                         :order       20}
          :clientSecret {:displayName "Client Secret"
                         :type        "password"
                         :description "client secret assigned by the MITREid server"
                         :mandatory   false
                         :readOnly    false
                         :order       21}
          :baseURL      {:displayName "Base URL"
                         :type        "string"
                         :description "server's endpoint URL for the MITREid protocol"
                         :mandatory   false
                         :readOnly    false
                         :order       22}
          :publicKey    {:displayName "Public Key"
                         :type        "string"
                         :description "public key to verify signed tokens from the MITREid server"
                         :mandatory   true
                         :readOnly    false
                         :order       23}
          :authorizeURL {:displayName "Authorize URL"
                         :type        "string"
                         :description "server's endpoint authentication URL for the MITREid protocol"
                         :mandatory   false
                         :readOnly    false
                         :order       24}
          :tokenURL     {:displayName "Token URL"
                         :type        "string"
                         :description "server's endpoint token URL for the MITREid protocol"
                         :mandatory   false
                         :readOnly    false
                         :order       25}}))

;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::ct-mitreid/session-mitreid))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))
