(ns com.sixsq.slipstream.ssclj.resources.spec.credential-cloud-dummy
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential :as cred]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud :as ctc]))

(s/def :cimi.credential.cloud-dummy/domain-name string?)

(def credential-keys-spec
  (update-in ctc/credential-template-cloud-keys-spec
    [:opt-un] concat [:cimi.credential.cloud-dummy/domain-name]))

(s/def :cimi/credential.cloud-dummy
  (su/only-keys-maps cred/credential-keys-spec
                     credential-keys-spec))

(s/def :cimi/credential.cloud-dummy.create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.cloud-dummy/credentialTemplate]}))
