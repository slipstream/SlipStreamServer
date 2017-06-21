(ns com.sixsq.slipstream.ssclj.resources.spec.usage-summary
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.usage-summary/user :cimi.core/nonblank-string)
(s/def :cimi.usage-summary/cloud :cimi.core/nonblank-string)
(s/def :cimi.usage-summary/start-timestamp :cimi.core/timestamp)
(s/def :cimi.usage-summary/end-timestamp :cimi.core/timestamp)
(s/def :cimi.usage-summary/usage any?)
(s/def :cimi.usage-summary/grouping :cimi.core/nonblank-string)
(s/def :cimi.usage-summary/frequency :cimi.core/nonblank-string)

(s/def :cimi/usage-summary
  (su/only-keys :req-un [:cimi.common/id
                         :cimi.common/resourceURI
                         :cimi.common/created
                         :cimi.common/updated
                         :cimi.common/acl

                         :cimi.usage-summary/user
                         :cimi.usage-summary/cloud
                         :cimi.usage-summary/start-timestamp
                         :cimi.usage-summary/end-timestamp
                         :cimi.usage-summary/usage
                         :cimi.usage-summary/grouping
                         :cimi.usage-summary/frequency]
                :opt-un [:cimi.common/name
                         :cimi.common/description
                         :cimi.common/properties
                         :cimi.common/operations]))
