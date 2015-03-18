(ns com.sixsq.slipstream.ssclj.resources.connector-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.connector :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id          resource-name
            :resourceURI c/service-context
            :created     timestamp
            :updated     timestamp
            :acl         valid-acl
            :serviceName     "http://cloud.example.org/"}]

  (expect nil? (s/check Connector root))
  (expect (s/check Connector (dissoc root :created)))
  (expect (s/check Connector (dissoc root :updated)))
  (expect (s/check Connector (dissoc root :serviceName)))
  (expect (s/check Connector (dissoc root :acl))))


(run-tests [*ns*])

