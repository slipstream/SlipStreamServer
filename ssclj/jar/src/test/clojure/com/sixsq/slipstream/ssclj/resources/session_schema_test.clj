(ns com.sixsq.slipstream.ssclj.resources.session-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.session :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      root      {:id          resource-name
                 :resourceURI p/service-context
                 :created     timestamp
                 :updated     timestamp
                 :acl         valid-acl
                 :owner       "Legal Person"
                 :type        "CloudX Connector"
                 :expiry      "2020-08-25T00:00:00.000Z"
                 :userLimit   0}]

  (expect nil? (s/check Session root))
  (expect (s/check Session (dissoc root :created)))
  (expect (s/check Session (dissoc root :updated)))
  (expect (s/check Session (dissoc root :acl)))

  (expect (s/check Session (dissoc root :owner)))
  (expect (s/check Session (dissoc root :type)))

  (expect (s/check Session (dissoc root :expiry)))
  (expect (s/check Session (assoc root :expiry "invalid timestamp")))

  (expect (s/check Session (dissoc root :userLimit)))
  (expect (s/check Session (assoc root :userLimit -1)))
  (expect (s/check Session (assoc root :userLimit "a")))
  (expect nil? (s/check Session (assoc root :userLimit 1000))))


(run-tests [*ns*])

