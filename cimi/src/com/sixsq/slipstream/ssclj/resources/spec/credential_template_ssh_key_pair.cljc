(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-key-pair
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]))

(s/def :cimi.credential-template.ssh-key-pair/size pos-int?)

(s/def :cimi.credential-template.ssh-key-pair/algorithm #{"rsa" "dsa"})

(def credential-template-keys-spec
  {:opt-un [:cimi.credential-template.ssh-key-pair/size
            :cimi.credential-template.ssh-key-pair/algorithm]})

(def credential-template-create-keys-spec
  {:opt-un [:cimi.credential-template.ssh-key-pair/size
            :cimi.credential-template.ssh-key-pair/algorithm]})

;; Defines the contents of the ssh-key-pair CredentialTemplate resource itself.
(s/def :cimi/credential-template.ssh-key-pair
  (su/only-keys-maps ps/resource-keys-spec
                     credential-template-keys-spec))

;; Defines the contents of the ssh-key-pair template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def :cimi.credential-template.ssh-key-pair/credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     credential-template-create-keys-spec))

(s/def :cimi/credential-template.ssh-key-pair-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.ssh-key-pair/credentialTemplate]}))
