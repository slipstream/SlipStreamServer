(ns com.sixsq.slipstream.ssclj.resources.spec.deployment
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::state string?)

(s/def ::model ::cimi-common/resource-link)

(def deployment-keys-spec
  (su/merge-keys-specs [cimi-common/common-attrs
                        {:req-un [::state ::model]}]))

(s/def ::deployment (su/only-keys-maps deployment-keys-spec))
