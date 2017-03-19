(ns com.sixsq.slipstream.ssclj.resources.session-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.session :refer :all]
    [schema.core :as s]
    [clojure.test :refer [is]]))

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
           :authnMethod "internal"
           :username    "ssuser"
           :virtualHost "nuv.la"
           :clientIP    "127.0.0.1"
           :expiry      timestamp}]

  (is (nil? (s/check Session cfg)))
  (is (s/check Session (dissoc cfg :created)))
  (is (s/check Session (dissoc cfg :updated)))
  (is (s/check Session (dissoc cfg :acl))))
