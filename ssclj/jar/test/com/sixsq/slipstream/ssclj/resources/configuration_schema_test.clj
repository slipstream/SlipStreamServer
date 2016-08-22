(ns com.sixsq.slipstream.ssclj.resources.configuration-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.configuration :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      cfg (merge default-configuration
                 {:id          (str resource-name "/slipstream")
                  :resourceURI resource-uri
                  :created     timestamp
                  :updated     timestamp
                  :acl         valid-acl})]

  (expect nil? (s/check Configuration cfg))
  (expect (s/check Configuration (dissoc cfg :created)))
  (expect (s/check Configuration (dissoc cfg :updated)))
  (expect (s/check Configuration (dissoc cfg :acl))))
