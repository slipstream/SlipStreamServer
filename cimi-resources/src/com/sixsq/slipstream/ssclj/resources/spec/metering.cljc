(ns com.sixsq.slipstream.ssclj.resources.spec.metering
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.virtual-machine :as vm]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def :cimi.metering/snapshot-time ::cimi-core/timestamp)


(def metering-keys-spec (su/merge-keys-specs [c/common-attrs
                                              vm/virtual-machine-specs
                                              {:req-un [:cimi.metering/snapshot-time]}]))

(s/def :cimi/metering (su/only-keys-maps metering-keys-spec))
