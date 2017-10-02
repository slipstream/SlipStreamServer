(ns com.sixsq.slipstream.ssclj.resources.spec.quota-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.quota :as sq]))

(def valid? (partial s/valid? :cimi/quota))
(def invalid? (complement valid?))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "jane"
                         :type      "USER"
                         :right     "VIEW"}]})

(deftest check-quota
  (let [timestamp "1964-08-25T10:00:00.0Z"
        quota {:id          (str sq/resource-url "/test-quota")
               :name        "Test Quota"
               :description "An example quota with a value."
               :resourceURI sq/resource-uri
               :created     timestamp
               :updated     timestamp
               :acl         valid-acl

               :resource    "VirtualMachine"
               :selection   "organization='cern'"
               :aggregation "count:id"
               :limit       100}]

    (are [expect-fn arg] (expect-fn arg)
                         valid? quota
                         valid? (dissoc quota :name)
                         valid? (dissoc quota :description)
                         invalid? (dissoc quota :resourceURI)
                         invalid? (dissoc quota :created)
                         invalid? (dissoc quota :updated)
                         invalid? (dissoc quota :acl)

                         invalid? (dissoc quota :resource)
                         invalid? (assoc quota :resource 0)
                         invalid? (assoc quota :resource "")

                         invalid? (dissoc quota :selection)
                         invalid? (assoc quota :selection 0)
                         invalid? (assoc quota :selection "")
                         invalid? (assoc quota :selection "BAD==='FILTER'")

                         invalid? (dissoc quota :aggregation)
                         invalid? (assoc quota :aggregation 0)
                         invalid? (assoc quota :aggregation "")

                         invalid? (dissoc quota :limit)
                         invalid? (assoc quota :limit 0)
                         invalid? (assoc quota :limit ""))))
