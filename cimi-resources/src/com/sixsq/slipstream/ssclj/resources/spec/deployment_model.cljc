(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-model
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::module ::cimi-common/resource-link)

(s/def ::credential ::cimi-common/resource-link)

(s/def ::nodeID ::cimi-core/token)

(s/def ::cpu nat-int?)

(s/def ::ram nat-int?)

(s/def ::disk nat-int?)

(s/def ::node (su/only-keys :req-un [::nodeID ::credential]
                            :opt-un [::cpu ::ram ::disk]))

(s/def ::nodes (s/coll-of ::node :kind vector? :min-count 1))


(def deployment-model-keys-spec
  (su/merge-keys-specs [cimi-common/common-attrs
                        {:req-un [::module ::nodes]}]))

(s/def ::deployment-model (su/only-keys-maps deployment-model-keys-spec))
