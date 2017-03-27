(ns com.sixsq.slipstream.ssclj.resources.service-offer-schema-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.service-offer :refer :all]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def non-nil? (complement nil?))

(deftest check-ServiceInfo
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          resource-name
              :resourceURI p/service-context
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl
              :connector   {:href "myconnector"}
              :other       "value"}]

    (are [expect-fn arg] (expect-fn (s/check ServiceInfo arg))
                         nil? root
                         non-nil? (dissoc root :created)
                         non-nil? (dissoc root :updated)
                         non-nil? (dissoc root :acl)
                         non-nil? (dissoc root :connector)
                         nil? (dissoc root :other))))
