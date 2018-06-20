(ns com.sixsq.slipstream.ssclj.resources.spec.evidence
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]  
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


; (s/def :cimi.evidence/credentials (s/coll-of ::cimi-common/resource-link))
(s/def :cimi.evidence/class ::cimi-core/nonblank-string)
(s/def :cimi.evidence/endTime ::cimi-core/timestamp)
(s/def :cimi.evidence/startTime ::cimi-core/timestamp)
(s/def :cimi.evidence/planID ::cimi-core/nonblank-string)
(s/def :cimi.evidence/passed boolean?)

(s/def :cimi/evidence
  (su/constrained-map keyword? any?
                      cimi-common/common-attrs
                      {:req-un [:cimi.evidence/endTime
                                :cimi.evidence/startTime
                                :cimi.evidence/planID
                                :cimi.evidence/passed
                                :cimi.evidence/class]}))
