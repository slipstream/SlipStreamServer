(ns com.sixsq.slipstream.ssclj.resources.spec.credential-api-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-api-key]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]))

(s/def :cimi.credential.api-key/expiry :cimi.core/timestamp)

(s/def :cimi.credential.api-key/digest :cimi.core/nonblank-string)

(s/def :cimi.credential.api-key/identity :cimi.core/nonblank-string)

(s/def :cimi.credential.api-key/roles (s/coll-of :cimi.core/nonblank-string
                                                 :kind vector?
                                                 :into []
                                                 :min-count 1))

(s/def :cimi.credential.api-key/claims (su/only-keys :req-un [:cimi.credential.api-key/identity]
                                                     :opt-un [:cimi.credential.api-key/roles]))

(def credential-keys-spec
  {:req-un [:cimi.credential.api-key/digest
            :cimi.credential.api-key/claims]
   :opt-un [:cimi.credential.api-key/expiry]})

(s/def :cimi/credential.api-key
  (su/only-keys-maps cred/credential-keys-spec
                     credential-keys-spec))

;; multiple methods to create an ssh public key, so multiple schemas
(s/def :cimi/credential.api-key.create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.api-key/credentialTemplate]}))
