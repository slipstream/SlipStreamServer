(ns com.sixsq.slipstream.ssclj.resources.spec.virtual-machine
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.virtual-machine/cloud :cimi.common/resource-link)
(s/def :cimi.virtual-machine/user :cimi.common/resource-link)
(s/def :cimi.virtual-machine/instanceId :cimi.core/identifier)
(s/def :cimi.virtual-machine/instanceType :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine/state :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine/ip :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine/name :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine/nodeName :cimi.core/nonblank-string)
(s/def :cimi.virtual-machine/nodeInstanceId :cimi.core/identifier)
(s/def :cimi.virtual-machine/usable boolean?)
(s/def :cimi.virtual-machine/cpu pos-int?)
(s/def :cimi.virtual-machine/ram pos-int?)
(s/def :cimi.virtual-machine/disk pos-int?)
(s/def :cimi.virtual-machine/serviceOffer :cimi.common/resource-link)
(s/def :cimi.virtual-machine/run :cimi.common/resource-link)

(def vm-specs {:req-un [:cimi.virtual-machine/cloud
                        :cimi.virtual-machine/instanceId
                        :cimi.virtual-machine/state]
               :opt-un [:cimi:vm/run
                        :cimi:vm/serviceOffer
                        :cimi.virtual-machine/ip
                        :cimi.virtual-machine/name
                        :cimi.virtual-machine/nodeName
                        :cimi.virtual-machine/nodeInstanceId
                        :cimi.virtual-machine/usable]})


(def vm-keys-spec (su/merge-keys-specs [c/common-attrs
                                        vm-specs]))

(s/def :cimi/virtual-machine (su/only-keys-maps vm-keys-spec))
