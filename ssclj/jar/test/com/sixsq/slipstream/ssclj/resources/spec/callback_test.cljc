(ns com.sixsq.slipstream.ssclj.resources.spec.callback-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.callback :as sc]))

(def valid? (partial s/valid? :cimi/callback))
(def invalid? (complement valid?))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ADMIN"
                         :type      "ROLE"
                         :right     "MODIFY"}]})


(deftest check-callback
  (let [timestamp "1964-08-25T10:00:00.0Z"
        callback {:id          (str sc/resource-url "/test-callback")
                  :resourceURI sc/resource-uri
                  :created     timestamp
                  :acl         valid-acl
                  :updated     timestamp
                  :state       "WAITING"
                  :action      "validate-something"}]
    (are [expect-fn arg] (expect-fn arg)
                         valid? callback
                         valid? (assoc callback :state "SUCCESS")
                         valid? (assoc callback :data {:a "a" :b 1 :c nil})
                         invalid? (assoc callback :data "should be a map")
                         invalid? (assoc callback :state "XY")
                         )))
