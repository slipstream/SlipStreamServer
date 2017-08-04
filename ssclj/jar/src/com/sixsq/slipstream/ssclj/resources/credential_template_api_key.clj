(ns com.sixsq.slipstream.ssclj.resources.credential-template-api-key
  "This creates an API key that can be used to log into the server."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-api-key]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const credential-type "api-key")
(def ^:const method "generate-api-key")

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;
(def ^:const resource
  {:type        credential-type
   :method      method
   :name        "Generate API Key"
   :description "generates an API key and stores hash"
   :acl         resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/CredentialTemplateDescription
         {:expiry {:displayName "Time to Live (TTL)"
                   :category    "general"
                   :description "number of seconds before the API key expires"
                   :type        "int"
                   :mandatory   false
                   :readOnly    false
                   :order       1}}))

;;
;; initialization: register this Credential template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/credential-template.api-key))
(defmethod p/validate-subtype method
  [resource]
  (validate-fn resource))
