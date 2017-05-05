(ns com.sixsq.slipstream.ssclj.resources.credential-template-ssh-public-key
  "This CredentialTemplate allows creating a Credential containing an SSH public key."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-public-key]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const credential-type "ssh-public-key")

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;
(def ^:const resource
  {:type         credential-type
   :name         "SSH Public Key"
   :description  "public key of an SSH key pair"
   :sshPublicKey "ssh-public-key"
   :acl          resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/CredentialTemplateDescription
         {:sshPublicKey {:displayName "SSH Public Key"
                         :category    "general"
                         :description "public key of an SSH key pair"
                         :type        "string"
                         :mandatory   true
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

(def validate-fn (u/create-spec-validation-fn :cimi/credential-template.ssh-public-key))
(defmethod p/validate-subtype credential-type
  [resource]
  (validate-fn resource))
