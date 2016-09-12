(ns com.sixsq.slipstream.ssclj.resources.connector-template-schema-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as s]

    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as ct]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root      {:id                  ct/resource-name
                   :resourceURI         p/service-context
                   :created             timestamp
                   :updated             timestamp
                   :acl                 valid-acl
                   :cloudServiceType    "CloudSoftwareSolution"
                   :orchestratorImageid "123"
                   :quotaVm             "0"
                   :maxIaasWorkers      20
                   :instanceName        "foo"}]
    (is (nil? (s/check ct/ConnectorTemplate root)))
    (doseq [k (into #{} (keys (dissoc root :id :resourceURI)))]
      (is (not (nil? (s/check ct/ConnectorTemplate (dissoc root k))))))))
