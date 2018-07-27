(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.deployment :as d]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment :as ds]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def timestamp "1964-08-25T10:00:00.0Z")


(def valid-model {:id          (str d/resource-url "/connector-uuid")
                  :resourceURI d/resource-uri
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
                                 :disk       300}]})


(def valid-deployment {:id          (str d/resource-url "/connector-uuid")
                       :resourceURI d/resource-uri
                       :created     timestamp
                       :updated     timestamp
                       :acl         valid-acl

                       :state       "RUNNING"
                       :model       (merge {:href "my-model-uuid"} valid-model)})


(deftest test-schema-check
  (stu/is-valid ::ds/deployment valid-deployment)
  (stu/is-invalid ::ds/deployment (assoc valid-deployment :badKey "badValue"))
  (stu/is-invalid ::ds/deployment (assoc valid-deployment :module "must-be-href"))

  ;; required attributes
  (doseq [k #{:id :resourceURI :created :updated :acl :state :model}]
    (stu/is-invalid ::ds/deployment (dissoc valid-deployment k))))
