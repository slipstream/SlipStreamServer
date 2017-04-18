(ns com.sixsq.slipstream.ssclj.resources.spec.usage
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.usage/user :cimi.core/nonblank-string)
(s/def :cimi.usage/cloud :cimi.core/nonblank-string)
(s/def :cimi.usage/start-timestamp :cimi.core/timestamp)
(s/def :cimi.usage/end-timestamp :cimi.core/timestamp)
(s/def :cimi.usage/usage :cimi.core/nonblank-string)
(s/def :cimi.usage/grouping :cimi.core/nonblank-string)
(s/def :cimi.usage/frequency :cimi.core/nonblank-string)

(s/def :cimi/usage
  (su/only-keys :req-un [:cimi.common/id
                         :cimi.common/resourceURI
                         :cimi.common/created
                         :cimi.common/updated
                         :cimi.common/acl

                         :cimi.usage/user
                         :cimi.usage/cloud
                         :cimi.usage/start-timestamp
                         :cimi.usage/end-timestamp
                         :cimi.usage/usage
                         :cimi.usage/grouping
                         :cimi.usage/frequency]
                :opt-un [:cimi.common/name
                         :cimi.common/description
                         :cimi.common/properties
                         :cimi.common/operations]))
