(ns com.sixsq.slipstream.ssclj.resources.configuration-schema-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as tpl]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest test-configuration-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str resource-name "/slipstream")
             :resourceURI resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl
             :service     "foo"}
        check (s/checker Configuration)]
    (is (nil? (check cfg)))
    (is (check (dissoc cfg :created)))
    (is (check (dissoc cfg :updated)))
    (is (check (dissoc cfg :acl)))))
