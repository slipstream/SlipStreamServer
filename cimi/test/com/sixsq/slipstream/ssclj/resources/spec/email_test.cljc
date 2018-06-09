(ns com.sixsq.slipstream.ssclj.resources.spec.email-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.email :as t]
    [com.sixsq.slipstream.ssclj.resources.spec.email :as email]))

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

    (is (s/valid? ::email/email email))
    (is (not (s/valid? ::email/email (assoc email :bad "value"))))
    (doseq [attr #{:id :resourceURI :created :updated :acl :address :validated?}]
      (is (not (s/valid? ::email/email (dissoc email attr)))))))
