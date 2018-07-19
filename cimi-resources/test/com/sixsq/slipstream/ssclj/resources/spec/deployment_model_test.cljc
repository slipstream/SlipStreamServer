(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-model-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.module :as t]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-model :as dm]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          (str t/resource-url "/connector-uuid")
              :resourceURI t/resource-uri
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl

              :module      {:href "my-model-uuid"}

              :nodes       [{:nodeID     "my-node-uuid"
                             :credential {:href "my-cred-uuid"}
                             :cpu        10
                             :ram        20
                             :disk       30}
                            {:nodeID     "my-second-node-uuid"
                             :credential {:href "my-second-cred-uuid"}
                             :cpu        100
                             :ram        200
                             :disk       300}]}]

    (stu/is-valid ::dm/deployment-model root)
    (stu/is-invalid ::dm/deployment-model (assoc root :badKey "badValue"))
    (stu/is-invalid ::dm/deployment-model (assoc root :module "must-be-href"))

    ;; required attributes
    (doseq [k #{:id :resourceURI :created :updated :acl :module :nodes}]
      (stu/is-invalid ::dm/deployment-model (dissoc root k)))))
