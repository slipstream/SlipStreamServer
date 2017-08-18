(ns com.sixsq.slipstream.ssclj.resources.spec.vm
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.vm/connector :cimi.common/resource-link)
(s/def :cimi.vm/user :cimi.common/resource-link)
(s/def :cimi.vm/instanceId :cimi.core/identifier)
(s/def :cimi.vm/instanceType :cimi.core/nonblank-string)
(s/def :cimi.vm/state :cimi.core/nonblank-string)
(s/def :cimi.vm/ip :cimi.core/nonblank-string)
(s/def :cimi.vm/name :cimi.core/nonblank-string)
(s/def :cimi.vm/nodeName :cimi.core/nonblank-string)
(s/def :cimi.vm/nodeInstanceId :cimi.core/identifier)
(s/def :cimi.vm/usable boolean?)
(s/def :cimi.vm/cpu pos-int?)
(s/def :cimi.vm/ram pos-int?)
(s/def :cimi.vm/disk pos-int?)
(s/def :cimi.vm/serviceOffer :cimi.common/resource-link)
(s/def :cimi.vm/run :cimi.common/resource-link)


(def vm-specs {:req-un [:cimi.vm/connector
                        :cimi.vm/user
                        :cimi.vm/instanceId
                        :cimi.vm/state
                        ]
               :opt-un [:cimi:vm/run
                        :cimi:vm/serviceOffer
                        :cimi.vm/ip
                        :cimi.vm/name
                        :cimi.vm/nodeName
                        :cimi.vm/nodeInstanceId
                        :cimi.vm/usable
                        ]})


(def vm-keys-spec (su/merge-keys-specs [c/common-attrs
                                        vm-specs]))

(s/def :cimi/vm (su/only-keys-maps vm-keys-spec))