(ns com.sixsq.slipstream.ssclj.resources.spec.credential-ssh-public-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-public-key]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-key-pair]))

(s/def :cimi.credential.ssh-public-key/algorithm #{"rsa" "dsa"})

(s/def :cimi.credential.ssh-public-key/fingerprint (s/and string? #(re-matches #"^[0-9a-f]{2}(:[0-9a-f]{2}){15}$" %)))

(s/def :cimi.credential.ssh-public-key/publicKey :cimi.credential-template.ssh-public-key/publicKey)

(def credential-keys-spec
  {:req-un [:cimi.credential.ssh-public-key/algorithm
            :cimi.credential.ssh-public-key/fingerprint
            :cimi.credential.ssh-public-key/publicKey]})

(s/def :cimi/credential.ssh-public-key
  (su/only-keys-maps cred/credential-keys-spec
                     credential-keys-spec))

;; multiple methods to create an ssh public key, so multiple schemas
(s/def :cimi/credential.ssh-public-key.create
  (s/or :import :cimi/credential-template.ssh-public-key-create
        :generate :cimi/credential-template.ssh-key-pair-create))
