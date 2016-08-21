(ns com.sixsq.slipstream.ssclj.resources.connector-template-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.connector-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id               resource-name
            :resourceURI      p/service-context
            :created          timestamp
            :updated          timestamp
            :acl              valid-acl
            :cloudServiceType "CloudSoftwareSolution"}]

  (expect nil? (s/check ConnectorTemplate root))
  (expect (s/check ConnectorTemplate (dissoc root :created)))
  (expect (s/check ConnectorTemplate (dissoc root :updated)))
  (expect (s/check ConnectorTemplate (dissoc root :acl)))
  (expect (s/check ConnectorTemplate (dissoc root :cloudServiceType))))

(run-tests [*ns*])

