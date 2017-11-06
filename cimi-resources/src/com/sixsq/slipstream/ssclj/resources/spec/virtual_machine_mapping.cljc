(ns com.sixsq.slipstream.ssclj.resources.spec.virtual-machine-mapping
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.virtual-machine-mapping/cloud :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine-mapping/instanceID :cimi.core/nonblank-string)

(s/def :cimi.virtual-machine-mapping/runUUID :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine-mapping/owner :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine-mapping/serviceOffer :cimi.common/resource-link)

(def virtual-machine-mapping-specs {:req-un [:cimi.virtual-machine-mapping/cloud
                                             :cimi.virtual-machine-mapping/instanceID]
                                    :opt-un [:cimi.virtual-machine-mapping/runUUID
                                             :cimi.virtual-machine-mapping/owner
                                             :cimi.virtual-machine-mapping/serviceOffer]})

(def virtual-machine-mapping-keys-spec (su/merge-keys-specs [c/common-attrs
                                                             virtual-machine-mapping-specs]))

(s/def :cimi/virtual-machine-mapping (su/only-keys-maps virtual-machine-mapping-keys-spec))
