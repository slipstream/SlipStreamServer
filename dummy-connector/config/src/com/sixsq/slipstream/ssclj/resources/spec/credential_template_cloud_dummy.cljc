(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud :as ctc]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::domain-name string?)


(def credential-template-keys-spec
  {:opt-un [::domain-name]})


(def credential-template-create-keys-spec credential-template-keys-spec)


;; Defines the contents of the cloud-dummy CredentialTemplate resource itself.
(s/def ::credential-template
  (su/only-keys-maps ps/resource-keys-spec
                     ctc/credential-template-cloud-keys-spec
                     credential-template-keys-spec))


;; Defines the contents of the cloud-dummy template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def ::credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     ctc/credential-template-create-keys-spec
                     credential-template-create-keys-spec))


(s/def ::credential-template-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::credentialTemplate]}))
