(ns com.sixsq.slipstream.ssclj.resources.configuration-template-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.configuration-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id          resource-name
            :resourceURI p/service-context
            :created     timestamp
            :updated     timestamp
            :acl         valid-acl
            :service     "CloudSoftwareSolution"}]

  (expect nil? (s/check ConfigurationTemplate root))
  (expect (s/check ConfigurationTemplate (dissoc root :created)))
  (expect (s/check ConfigurationTemplate (dissoc root :updated)))
  (expect (s/check ConfigurationTemplate (dissoc root :acl)))
  (expect (s/check ConfigurationTemplate (dissoc root :cloudServiceType))))
