(ns com.sixsq.slipstream.ssclj.resources.connector-schema-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as s]

    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.connector :as c]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root      {:id                  c/resource-name
                   :resourceURI         p/service-context
                   :created             timestamp
                   :updated             timestamp
                   :acl                 valid-acl
                   :cloudServiceType    "alpha"
                   :orchestratorImageid "123"
                   :quotaVm             "20"
                   :maxIaasWorkers      5
                   :instanceName        "foo"}]
    (is (nil? (s/check c/Connector root)))
    (doseq [k (into #{} (keys (dissoc root :id :resourceURI)))]
      (is (not (nil? (s/check c/Connector (dissoc root k))))))))
