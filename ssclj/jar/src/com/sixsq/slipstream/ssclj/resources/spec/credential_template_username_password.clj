(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-username-password
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]))

(s/def :cimi.credential-template.username-password/username :cimi.core/nonblank-string)
(s/def :cimi.credential-template.username-password/password :cimi.core/nonblank-string)

(def credential-template-keys-spec-req
  {:req-un [:cimi.credential-template.username-password/username
            :cimi.credential-template.username-password/password]})

(def credential-template-create-keys-spec-req
  {:req-un [:cimi.credential-template.username-password/username
            :cimi.credential-template.username-password/password]})

;; Defines the contents of the username-password CredentialTemplate resource itself.
(s/def :cimi/credential-template.username-password
  (su/only-keys-maps ps/resource-keys-spec
                     credential-template-keys-spec-req))

;; Defines the contents of the username-password template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def :cimi.credential-template.username-password/credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     credential-template-create-keys-spec-req))

(s/def :cimi/credential-template.username-password-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.credential-template.username-password/credentialTemplate]}))
