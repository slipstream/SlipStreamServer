(ns com.sixsq.slipstream.ssclj.resources.spec.service-benchmark-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.service-benchmark :as sb]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest check-ServiceInfo
  (let [timestamp "1964-08-25T10:00:00.0Z"
        service-benchmark {:id          (str sb/resource-url "/bmk-uuid")
                       :resourceURI sb/resource-uri
                       :created     timestamp
                       :updated     timestamp
                       :acl         valid-acl
                       :connector   {:href "myconnector"}
                       :other       "value"}]

    (are [expect-fn arg] (expect-fn (s/valid? :cimi/service-benchmark arg))
                         true? service-benchmark
                         false? (dissoc service-benchmark :created)
                         false? (dissoc service-benchmark :updated)
                         false? (dissoc service-benchmark :acl)
                         false? (dissoc service-benchmark :connector)
                         true? (dissoc service-benchmark :other))))
