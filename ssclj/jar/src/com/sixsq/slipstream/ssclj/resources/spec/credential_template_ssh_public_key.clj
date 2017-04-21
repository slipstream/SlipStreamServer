(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-public-key
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]))

(s/def :cimi.credential-template.ssh-public-key/sshPublicKey :cimi.core/nonblank-string)

(def credential-template-keys-spec-req
  {:req-un [:cimi.credential-template.ssh-public-key/sshPublicKey]})

(def credential-template-create-keys-spec-req
  {:req-un [:cimi.credential-template.ssh-public-key/sshPublicKey]})

;; Defines the contents of the ssh-public-key CredentialTemplate resource itself.
(s/def :cimi/credential-template.ssh-public-key
  (su/only-keys-maps ps/resource-keys-spec
                     credential-template-keys-spec-req))

;; Defines the contents of the ssh-public-key template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def :cimi.credential-template.ssh-public-key/credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     credential-template-create-keys-spec-req))

(s/def :cimi/credential-template.ssh-public-key-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.credential-template.ssh-public-key/credentialTemplate]}))
