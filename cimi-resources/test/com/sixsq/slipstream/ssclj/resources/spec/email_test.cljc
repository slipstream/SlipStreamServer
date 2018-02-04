(ns com.sixsq.slipstream.ssclj.resources.spec.email-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.email :as t]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest check-email-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        email {:id          (str t/resource-url "/abcdef")
               :resourceURI t/resource-uri
               :created     timestamp
               :updated     timestamp
               :acl         valid-acl
               :address     "user@example.com"
               :validated?  false}]

    (is (s/valid? :cimi/email email))
    (is (not (s/valid? :cimi/email (assoc email :bad "value"))))
    (doseq [attr #{:id :resourceURI :created :updated :acl :address :validated?}]
      (is (not (s/valid? :cimi/email (dissoc email attr)))))))
