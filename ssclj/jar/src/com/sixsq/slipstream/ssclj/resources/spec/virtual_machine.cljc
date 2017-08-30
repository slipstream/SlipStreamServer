(ns com.sixsq.slipstream.ssclj.resources.spec.virtual-machine
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.virtual-machine/credential :cimi.common/resource-link)
(s/def :cimi.virtual-machine/instanceID :cimi.core/identifier)
(s/def :cimi.virtual-machine/state :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine/ip :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine/serviceOffer :cimi.common/resource-link)
(s/def :cimi.virtual-machine/run :cimi.common/resource-link)

(def virtual-machine-specs {:req-un [:cimi.virtual-machine/credential
                                     :cimi.virtual-machine/instanceID
                                     :cimi.virtual-machine/state]
                            :opt-un [:cimi.virtual-machine/run
                                     :cimi.virtual-machine/serviceOffer
                                     :cimi.virtual-machine/ip]})

(def virtual-machine-keys-spec (su/merge-keys-specs [c/common-attrs
                                                     virtual-machine-specs]))

(s/def :cimi/virtual-machine (su/only-keys-maps virtual-machine-keys-spec))
