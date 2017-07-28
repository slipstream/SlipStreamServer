(ns com.sixsq.slipstream.ssclj.resources.spec.run-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.run :as r]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest check-RunInfo
  (let [timestamp "1964-08-25T10:00:00.0Z"
        run {:id                  (str r/resource-url "/run-uuid")
             :created             timestamp
             :updated             timestamp
             :resourceURI         r/resource-uri
             :acl                 valid-acl
             :module-resource-uri "module/examples/tutorials/service-testing/system/1940"
             :category            "Deployment"
             :type                "Orchestration"
             :mutable             false
             :nodes               {:node1 {:parameters         {:cloudservice {:description       "p1 description"
                                                                               :default-value     "abc"
                                                                               :user-choice-value "ABC"}
                                                                :multiplicity {:default-value "1"}}
                                           :runtime-parameters {:p1 {:description       "p1 description"
                                                                     :default-value     "abc"
                                                                     :user-choice-value "ABC"
                                                                     :mapped-to         "a"}}}
                                   :node2 {:parameters {:cloudservice {:description   "param1 description"
                                                                       :default-value "abc"}}}}}]

    (are [expect-fn arg] (expect-fn (s/valid? :cimi/run arg))
                         true? run
                         false? (dissoc run :id)
                         false? (dissoc run :created)
                         false? (dissoc run :updated)
                         false? (dissoc run :acl)
                         false? (dissoc run :module-resource-uri)
                         false? (dissoc run :category)
                         false? (dissoc run :type)
                         false? (update run :category (constantly "bonjour"))
                         false? (dissoc run :mutable)
                         false? (assoc run :other "abc")
                         false? (dissoc run :nodes)
                         true? (assoc run :start-time timestamp))))
