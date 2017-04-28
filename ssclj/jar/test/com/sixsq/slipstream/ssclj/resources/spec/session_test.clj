(ns com.sixsq.slipstream.ssclj.resources.spec.session-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.session :refer :all]
    [com.sixsq.slipstream.ssclj.resources.spec.session]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest check-session-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str resource-url "/internal")
             :resourceURI resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl
             :method      "internal"
             :username    "ssuser"
             :expiry      timestamp
             :server      "nuv.la"
             :clientIP    "127.0.0.1"}]

    (is (s/valid? :cimi/session cfg))
    (doseq [attr #{:id :resourceURI :created :updated :acl :method :username :expiry}]
      (is (not (s/valid? :cimi/session (dissoc cfg attr)))))
    (doseq [attr #{:server :clientIP}]
      (is (s/valid? :cimi/session (dissoc cfg attr))))))
