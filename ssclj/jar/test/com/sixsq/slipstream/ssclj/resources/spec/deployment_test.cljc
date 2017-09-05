(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.deployment :as d]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest check-DeploymentInfo
  (let [timestamp "1964-08-25T10:00:00.0Z"
        deployment {:id                  (str d/resource-url "/deployment-uuid")
                    :created             timestamp
                    :updated             timestamp
                    :resourceURI         d/resource-uri
                    :acl                 valid-acl
                    :module-resource-uri "module/examples/tutorials/service-testing/system/1940"
                    :category            "Deployment"
                    :type                "Orchestration"
                    :mutable             false
                    :nodes               {:node1
                                          {:parameters
                                           {:cloudservice {:description       "p1 description"
                                                           :default-value     "abc"
                                                           :user-choice-value "ABC"}
                                            :multiplicity {:default-value "1"}}
                                           :runtime-parameters
                                           {:p1 {:description       "p1 description"
                                                 :default-value     "abc"
                                                 :user-choice-value "ABC"
                                                 :mapped-to         "a"}}}
                                          :node2
                                          {:parameters
                                           {:cloudservice {:description   "param1 description"
                                                           :default-value "abc"}}}}}]

    (are [expect-fn arg] (expect-fn (s/valid? :cimi/deployment arg))
                         true? deployment
                         false? (dissoc deployment :id)
                         false? (dissoc deployment :created)
                         false? (dissoc deployment :updated)
                         false? (dissoc deployment :acl)
                         false? (dissoc deployment :module-resource-uri)
                         false? (dissoc deployment :category)
                         false? (dissoc deployment :type)
                         false? (update deployment :category (constantly "bonjour"))
                         false? (dissoc deployment :mutable)
                         false? (assoc deployment :other "abc")
                         false? (dissoc deployment :nodes)
                         true? (assoc deployment :start-time timestamp))))
