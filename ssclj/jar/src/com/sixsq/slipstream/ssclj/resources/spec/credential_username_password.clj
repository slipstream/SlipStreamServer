(ns com.sixsq.slipstream.ssclj.resources.spec.credential-username-password
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-username-password]))

(s/def :cimi.credential.username-password/username :cimi.credential-template.username-password/username)
(s/def :cimi.credential.username-password/password :cimi.credential-template.username-password/password)

(def credential-keys-spec
  {:req-un [:cimi.credential.username-password/username
            :cimi.credential.username-password/password]})

(s/def :cimi/credential.username-password
  (su/only-keys-maps ct/resource-keys-spec
                     credential-keys-spec))
