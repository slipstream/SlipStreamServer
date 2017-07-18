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
             :parameters          {:a "b"}
             :user-choices        {}}]

    (is (= true (s/valid? :cimi/run run)))

    (are [expect-fn arg] (expect-fn (s/valid? :cimi/run arg))
                         true? run
                         false? (dissoc run :created)
                         false? (dissoc run :updated)
                         false? (dissoc run :acl)
                         false? (dissoc run :module-resource-uri)
                         false? (dissoc run :category)
                         false? (dissoc run :type)
                         false? (update run :category (constantly "bonjour"))
                         false? (dissoc run :mutable)
                         false? (assoc run :other "abc")
                         true?  (assoc run :user-choices {:a "a" :b "b"})
                         false? (assoc run :user-choices {:a []})
                         false? (dissoc run :parameters)
                         true?  (assoc run :start-time timestamp))))
