(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-public-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common-namespaces :as common-ns]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def :cimi.credential-template.ssh-public-key/publicKey
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :name "publicKey"
             :json-schema/name "publicKey"
             :json-schema/namespace common-ns/slipstream-namespace
             :json-schema/uri common-ns/slipstream-uri
             :json-schema/type "string"
             :json-schema/providerMandatory true
             :json-schema/consumerMandatory true
             :json-schema/mutable true
             :json-schema/consumerWritable true

             :json-schema/displayName "public key"
             :json-schema/description "SSH public key"
             :json-schema/help "SSH public key"
             :json-schema/group "body"
             :json-schema/order 20
             :json-schema/hidden false
             :json-schema/sensitive false
             :json-schema/lines 3)))


(def credential-template-keys-spec
  {:req-un [:cimi.credential-template.ssh-public-key/publicKey]})

(def credential-template-create-keys-spec
  {:req-un [:cimi.credential-template.ssh-public-key/publicKey]})

;; Defines the contents of the ssh-public-key CredentialTemplate resource itself.
(s/def :cimi/credential-template.ssh-public-key
  (su/only-keys-maps ps/resource-keys-spec
                     credential-template-keys-spec))

;; Defines the contents of the ssh-public-key template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def :cimi.credential-template.ssh-public-key/credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     credential-template-create-keys-spec))

(s/def :cimi/credential-template.ssh-public-key-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.ssh-public-key/credentialTemplate]}))
