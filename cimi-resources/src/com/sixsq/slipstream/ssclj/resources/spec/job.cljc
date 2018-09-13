(ns com.sixsq.slipstream.ssclj.resources.spec.job
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.common-operation :as cimi-common-operation]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(def job-href-regex #"^job/[a-z]+(-[a-z]+)*$")
(s/def ::href (s/and string? #(re-matches job-href-regex %)))

(s/def ::state #{"QUEUED" "RUNNING" "FAILED" "SUCCESS" "STOPPING" "STOPPED"})
(s/def ::targetResource ::cimi-common/resource-link)
(s/def ::affectedResources ::cimi-common/resource-links)
(s/def ::returnCode int?)
(s/def ::progress (s/int-in 0 101))
(s/def ::timeOfStatusChange ::cimi-core/timestamp)
(s/def ::statusMessage string?)
(s/def ::action ::cimi-common-operation/rel)
(s/def ::parentJob ::href)
(s/def ::nestedJobs (s/coll-of ::href))
; An optional priority as an integer with at most 3 digits. Lower values signify higher priority.
(s/def ::priority (s/int-in 0 1000))


(s/def ::job
  (su/only-keys-maps cimi-common/common-attrs
                     {:req-un [::state
                               ::action
                               ::progress]
                      :opt-un [::targetResource
                               ::affectedResources
                               ::returnCode
                               ::statusMessage
                               ::timeOfStatusChange
                               ::parentJob
                               ::nestedJobs
                               ::priority]}))
