(ns com.sixsq.slipstream.ssclj.resources.spec.service-benchmark
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))


(s/def :cimi.service-benchmark/credentials
  (s/coll-of :cimi.common/resource-link))
(s/def :cimi.service-benchmark/serviceOffer :cimi.common/resource-link)

(s/def :cimi/service-benchmark
  (su/only-keys-maps c/common-attrs
                    {:req-un [:cimi.service-benchmark/credentials
                              :cimi.service-benchmark/serviceOffer]}))
