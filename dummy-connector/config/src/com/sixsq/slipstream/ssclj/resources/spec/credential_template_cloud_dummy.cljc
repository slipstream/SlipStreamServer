(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud :as ctc]))

(s/def :cimi.credential-template.cloud-dummy/domain-name string?)

(def credential-template-keys-spec
  {:opt-un [:cimi.credential-template.cloud-dummy/domain-name]})

(def credential-template-create-keys-spec credential-template-keys-spec)

;; Defines the contents of the cloud-dummy CredentialTemplate resource itself.
(s/def :cimi/credential-template.cloud-dummy
  (su/only-keys-maps ps/resource-keys-spec
                     ctc/credential-template-cloud-keys-spec
                     credential-template-keys-spec))

;; Defines the contents of the cloud-dummy template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :credentialTemplate here.
(s/def :cimi.credential-template.cloud-dummy/credentialTemplate
  (su/only-keys-maps ps/template-keys-spec
                     ctc/credential-template-create-keys-spec
                     credential-template-create-keys-spec))

(s/def :cimi/credential-template.cloud-dummy-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.credential-template.cloud-dummy/credentialTemplate]}))
