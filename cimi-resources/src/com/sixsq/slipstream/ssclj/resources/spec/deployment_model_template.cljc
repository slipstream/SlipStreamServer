(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-model-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::module ::cimi-common/resource-link)

(def deployment-model-template-keys-spec
  (su/merge-keys-specs [cimi-common/common-attrs
                        {:req-un [::module]}]))

(s/def ::deployment-model-template (su/only-keys-maps deployment-model-template-keys-spec))
