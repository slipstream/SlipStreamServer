(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-nuvlabox-identifier-test
  (:require
    [clojure.test :refer [deftest]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-nuvlabox-identifier :as cfg-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-configuration-template-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          (str ct/resource-url "/nuvlabox-identifier-test-instance")
              :resourceURI p/service-context
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl

              :service     "nuvlabox-identifier"
              :instance    "names"

              :identifiers  [{:name "John Doe"} {:name "Jane Dow"}] }]

    (stu/is-valid ::cfg-tpl/nuvlabox-identifier root)

    (stu/is-invalid ::cfg-tpl/nuvlabox-identifier (assoc root :bad "BAD"))

    (doseq [k #{:id :resourceURI :created :updated :acl
                :service :instance
                :identifiers}]
      (stu/is-invalid ::cfg-tpl/nuvlabox-identifier (dissoc root k)))))
