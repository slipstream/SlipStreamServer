(ns com.sixsq.slipstream.ssclj.resources.spec.credential-cloud-dummy
    (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]))

(s/def :cimi.credential.cloud-dummy/connector :cimi.common/resource-link)
(s/def :cimi.credential.cloud-dummy/key :cimi.core/nonblank-string)
(s/def :cimi.credential.cloud-dummy/secret :cimi.core/nonblank-string)
(s/def :cimi.credential.cloud-dummy/quota (s/or :pos-int pos-int? :zero zero?))
(s/def :cimi.credential.cloud-dummy/domain-name string?)

(def credential-keys-spec
  {:req-un [:cimi.credential.cloud-dummy/connector
            :cimi.credential.cloud-dummy/key
            :cimi.credential.cloud-dummy/secret
            :cimi.credential.cloud-dummy/quota]
   :opt-un [:cimi.credential.cloud-dummy/domain-name]})

(s/def :cimi/credential.cloud-dummy
  (su/only-keys-maps cred/credential-keys-spec
                     credential-keys-spec))

(s/def :cimi/credential.cloud-dummy.create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.cloud-dummy/credentialTemplate]}))
