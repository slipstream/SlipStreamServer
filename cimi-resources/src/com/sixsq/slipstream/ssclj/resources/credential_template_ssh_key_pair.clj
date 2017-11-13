(ns com.sixsq.slipstream.ssclj.resources.credential-template-ssh-key-pair
  "This CredentialTemplate allows creating a Credential containing the SSH
   public key from a generated SSH key pair."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-key-pair]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const credential-type "ssh-public-key")
(def ^:const method "generate-ssh-key-pair")

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
   :name        "Generate SSH Public Key"
   :description "public key of a generated SSH key pair"
   :size        1024
   :algorithm   "rsa"
   :acl         resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/CredentialTemplateDescription
         {:size      {:displayName "Size of SSH Key"
                      :category    "general"
                      :description "size in bits of generated key"
                      :type        "int"
                      :mandatory   false
                      :readOnly    false
                      :order       20}
          :algorithm {:displayName "SSH Key Algorithm"
                      :category    "general"
                      :description "algorithm ('rsa', 'dsa') to use to generate key pair"
                      :type        "string"
                      :mandatory   false
                      :readOnly    false
                      :order       21}}))

;;
;; initialization: register this Credential template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/credential-template.ssh-key-pair))
(defmethod p/validate-subtype method
  [resource]
  (validate-fn resource))
