(ns com.sixsq.slipstream.ssclj.resources.spec.usage-record
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.usage-record/cloud-vm-instanceid :cimi.core/nonblank-string)
(s/def :cimi.usage-record/user :cimi.core/nonblank-string)
(s/def :cimi.usage-record/cloud :cimi.core/nonblank-string)
(s/def :cimi.usage-record/metric-name :cimi.core/nonblank-string)
(s/def :cimi.usage-record/metric-value :cimi.core/nonblank-string)

(s/def :cimi.usage-record/start-timestamp :cimi.core/timestamp)
(s/def :cimi.usage-record/end-timestamp :cimi.core/optional-timestamp) ;; FIXME: Should just be timestamp.

(s/def :cimi/usage-record
  (su/only-keys :req-un [:cimi.common/id
                         :cimi.common/resourceURI
                         :cimi.common/acl

                         :cimi.usage-record/cloud-vm-instanceid
                         :cimi.usage-record/user
                         :cimi.usage-record/cloud
                         :cimi.usage-record/metric-name
                         :cimi.usage-record/metric-value]
                :opt-un [:cimi.common/name
                         :cimi.common/description
                         :cimi.common/created
                         :cimi.common/updated
                         :cimi.common/properties
                         :cimi.common/operations

                         :cimi.usage-record/start-timestamp
                         :cimi.usage-record/end-timestamp]))
