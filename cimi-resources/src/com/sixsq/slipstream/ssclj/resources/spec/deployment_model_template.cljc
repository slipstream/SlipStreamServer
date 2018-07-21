(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-model-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::module ::cimi-common/resource-link)

(def deployment-model-template-keys-spec
  (su/merge-keys-specs [cimi-common/common-attrs
                        {:req-un [::module]}]))

(s/def ::deployment-model-template (su/only-keys-maps deployment-model-template-keys-spec))

;; Defines the contents of the generic template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :deploymentModelTemplate here.
(s/def ::deploymentModelTemplate
  (su/only-keys-maps c/template-attrs
                     deployment-model-template-keys-spec))

(s/def ::deployment-model-template-create
  (su/only-keys-maps c/create-attrs
                     {:req-un [::deploymentModelTemplate]}))
