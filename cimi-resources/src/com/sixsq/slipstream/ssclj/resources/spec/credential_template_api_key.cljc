(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-api-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]))

(s/def :cimi.credential-template.api-key/ttl nat-int?)

(def credential-template-keys-spec
  {:opt-un [:cimi.credential-template.api-key/ttl]})

(def credential-template-create-keys-spec
  {:opt-un [:cimi.credential-template.api-key/ttl]})

;; Defines the contents of the api-key CredentialTemplate resource itself.
(s/def :cimi/credential-template.api-key
  (su/only-keys-maps ps/resource-keys-spec
                     credential-template-keys-spec))

;; Defines the contents of the api-key template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def :cimi.credential-template.api-key/credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     credential-template-create-keys-spec))

(s/def :cimi/credential-template.api-key-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.api-key/credentialTemplate]}))
