(ns com.sixsq.slipstream.ssclj.resources.spec.credential-ssh-public-key
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-ssh-public-key]))

(s/def :cimi.credential.ssh-public-key/sshPublicKey :cimi.credential-template.ssh-public-key/sshPublicKey)

(def credential-keys-spec
  {:req-un [:cimi.credential.ssh-public-key/sshPublicKey]})

(s/def :cimi/credential.ssh-public-key
  (su/only-keys-maps ct/resource-keys-spec
                     credential-keys-spec))
