(ns com.sixsq.slipstream.ssclj.resources.spec.callback-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.callback :as t]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ADMIN"
                         :type      "ROLE"
                         :right     "MODIFY"}]})


(deftest check-callback-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        callback {:id          (str t/resource-url "/test-callback")
                  :resourceURI t/resource-uri
                  :created     timestamp
                  :updated     timestamp
                  :acl         valid-acl
                  :action      "validate-something"
                  :state       "WAITING"
                  :resource    {:href "email/1230958abdef"}
                  :expires     timestamp
                  :data        {:some    "value"
                                :another "value"}}]

    (is (s/valid? :cimi/callback callback))
    (is (s/valid? :cimi/callback (assoc callback :state "SUCCEEDED")))
    (is (s/valid? :cimi/callback (assoc callback :state "FAILED")))
    (is (not (s/valid? :cimi/callback (assoc callback :state "UNKNOWN"))))
    (doseq [attr #{:id :resourceURI :created :updated :acl :action}]
      (is (not (s/valid? :cimi/callback (dissoc callback attr)))))
    (doseq [attr #{:expires :data}]
      (is (s/valid? :cimi/callback (dissoc callback attr))))))
