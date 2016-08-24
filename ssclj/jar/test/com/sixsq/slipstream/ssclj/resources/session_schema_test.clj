(ns com.sixsq.slipstream.ssclj.resources.session-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.session :refer :all]
    [com.sixsq.slipstream.ssclj.resources.session-template-internal :as tpl]
    [schema.core :as s]
    [expectations :refer :all]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      cfg {:id          (str resource-name "/internal")
           :resourceURI resource-uri
           :created     timestamp
           :updated     timestamp
           :acl         valid-acl
           :authnMethod "internal"}]

  (expect nil? (s/check Session cfg))
  (expect (s/check Session (dissoc cfg :created)))
  (expect (s/check Session (dissoc cfg :updated)))
  (expect (s/check Session (dissoc cfg :acl))))
