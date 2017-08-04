(ns com.sixsq.slipstream.ssclj.resources.spec.credential-api-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-api-key]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]))

(s/def :cimi.credential.api-key/expiry string?)

(s/def :cimi.credential.api-key/digest :cimi.core/nonblank-string)

(def credential-keys-spec
  {:req-un [:cimi.credential.api-key/digest]
   :opt-un [:cimi.credential.api-key/expiry]})

(s/def :cimi/credential.api-key
  (su/only-keys-maps cred/credential-keys-spec
                     credential-keys-spec))

;; multiple methods to create an ssh public key, so multiple schemas
(s/def :cimi/credential.api-key.create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.api-key/credentialTemplate]}))
