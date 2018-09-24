(ns com.sixsq.slipstream.ssclj.resources.credential-template-ssh-public-key
  "This CredentialTemplate allows creating a Credential containing an existing
   SSH public key, either in RSA or DSA format."
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-public-key]))

(def ^:const credential-type "ssh-public-key")
(def ^:const method "import-ssh-public-key")

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
   :name        "Import SSH Public Key"
   :description "import public key of an existing SSH key pair"
   :publicKey   "ssh-public-key"
   :acl         resource-acl
   :enabled     true
   })

;;
;; description
;;
(def ^:const desc
  (merge p/CredentialTemplateDescription
         {:publicKey {:displayName "SSH Public Key"
                      :category    "general"
                      :description "public RSA or DSA key of an SSH key pair"
                      :type        "string"
                      :mandatory   true
                      :readOnly    false
                      :order       20}}))

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
(defmethod p/validate-subtype method
  [resource]
  (validate-fn resource))
