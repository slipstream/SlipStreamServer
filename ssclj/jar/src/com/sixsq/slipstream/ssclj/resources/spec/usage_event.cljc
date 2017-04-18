(ns com.sixsq.slipstream.ssclj.resources.spec.usage-event
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.usage-event/cloud-vm-instanceid :cimi.core/nonblank-string)
(s/def :cimi.usage-event/user :cimi.core/nonblank-string)
(s/def :cimi.usage-event/cloud :cimi.core/nonblank-string)
(s/def :cimi.usage-event/start-timestamp :cimi.core/timestamp)
(s/def :cimi.usage-event/end-timestamp :cimi.core/optional-timestamp) ;; FIXME: Should just be timestamp

(s/def :cimi.usage-event/name :cimi.core/nonblank-string)
(s/def :cimi.usage-event/value :cimi.core/nonblank-string)
(s/def :cimi.usage-event/metric (su/only-keys :req-un [:cimi.usage-event/name
                                                       :cimi.usage-event/value]))
(s/def :cimi.usage-event/metrics (s/coll-of :cimi.usage-event/metric :min-count 1))

(s/def :cimi/usage-event
  (su/only-keys :req-un [:cimi.common/id
                         :cimi.common/resourceURI
                         :cimi.common/acl

                         :cimi.usage-event/cloud-vm-instanceid
                         :cimi.usage-event/user
                         :cimi.usage-event/cloud
                         :cimi.usage-event/metrics]
                :opt-un [:cimi.common/created               ;; FIXME: should be required
                         :cimi.common/updated               ;; FIXME: should be required
                         :cimi.common/name
                         :cimi.common/description
                         :cimi.common/properties
                         :cimi.common/operations

                         :cimi.usage-event/start-timestamp
                         :cimi.usage-event/end-timestamp]))
