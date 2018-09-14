(ns com.sixsq.slipstream.ssclj.resources.spec.credential-cloud-dummy
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-cloud :as cloud-cred]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud :as ctc]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy :as dummy-tpl]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::domain-name string?)


(def credential-keys-spec
  (update-in ctc/credential-template-cloud-keys-spec
    [:opt-un] concat [::domain-name]))


(s/def ::credential
  (su/only-keys-maps cloud-cred/credential-keys-spec
                     credential-keys-spec))


(s/def ::credential-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::dummy-tpl/credentialTemplate]}))
