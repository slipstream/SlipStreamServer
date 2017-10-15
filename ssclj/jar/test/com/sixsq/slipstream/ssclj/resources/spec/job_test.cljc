(ns com.sixsq.slipstream.ssclj.resources.spec.job-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.job :as sj]))

(def valid? (partial s/valid? :cimi/job))
(def invalid? (complement valid?))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "jane"
                         :type      "USER"
                         :right     "VIEW"}]})

(deftest check-job
  (let [timestamp "1964-08-25T10:00:00.0Z"
        job {:id          (str sj/resource-url "/test-quota")
             :resourceURI sj/resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl
             :state       "QUEUED"
             :progress    0
             :action      "add"
             :targetResource {:href "abc/def"}
             :affectedResources [{:href "abc/def"}]}]
    (are [expect-fn arg] (expect-fn arg)
                         valid? job
                         valid? (assoc job :parentJob "job/id")
                         valid? (assoc job :state "RUNNING")
                         valid? (assoc job :returnCode 10000)
                         invalid? (assoc job :progress 101)
                         invalid? (assoc job :state "XY")
                         invalid? (assoc job :parentJob "notjob/id")
                         )))
