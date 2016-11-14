(ns com.sixsq.slipstream.ssclj.resources.service-offer-schema-test
    (:require
    [com.sixsq.slipstream.ssclj.resources.service-offer :refer :all]
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
            :connector   {:href "myconnector"}
            :other       "value"}]

  (expect nil? (s/check ServiceInfo root))
  (expect (s/check ServiceInfo (dissoc root :created)))
  (expect (s/check ServiceInfo (dissoc root :updated)))
  (expect (s/check ServiceInfo (dissoc root :acl)))

  (expect (s/check ServiceInfo (dissoc root :connector)))
  (expect nil? (s/check ServiceInfo (dissoc root :other))))
