(ns com.sixsq.slipstream.ssclj.resources.spec.job
  (:require
    [clojure.spec.alpha :as s]
    [instaparse.core :as insta]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]))

(def job-href-regex #"^job/[a-z]+(-[a-z]+)*$")
(s/def :cimi.job/href (s/and string? #(re-matches job-href-regex %)))

(s/def :cimi.job/state #{"QUEUED" "RUNNING" "FAILED" "SUCCESS" "STOPPING" "STOPPED"})
(s/def :cimi.job/targetResource :cimi.common/resource-link)
(s/def :cimi.job/affectedResources :cimi.common/resource-links)
(s/def :cimi.job/returnCode int?)
(s/def :cimi.job/progress (s/int-in 0 101))
(s/def :cimi.job/timeOfStatusChange :cimi.core/timestamp)
(s/def :cimi.job/statusMessage string?)
(s/def :cimi.job/action :cimi.common.operation/rel)
(s/def :cimi.job/parentJob :cimi.job/href)
(s/def :cimi.job/nestedJobs (s/coll-of :cimi.job/href))


(s/def :cimi/job
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.job/state
                               :cimi.job/action
                               :cimi.job/progress]
                      :opt-un [:cimi.job/targetResource
                               :cimi.job/affectedResources
                               :cimi.job/returnCode
                               :cimi.job/statusMessage
                               :cimi.job/timeOfStatusChange
                               :cimi.job/parentJob
                               :cimi.job/nestedJobs]}))
