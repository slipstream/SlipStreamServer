(ns com.sixsq.slipstream.ssclj.resources.configuration-template-schema-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :refer :all]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest test-configuration-template-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id          resource-name
            :resourceURI p/service-context
            :created     timestamp
            :updated     timestamp
            :acl         valid-acl
            :service     "CloudSoftwareSolution"}
     check (s/checker ConfigurationTemplate)]
    (is (nil? (check root)))
    (is (check (dissoc root :created)))
    (is (check (dissoc root :updated)))
    (is (check (dissoc root :acl)))))
