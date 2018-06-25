(ns com.sixsq.slipstream.ssclj.resources.spec.user-identifier-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]
    [com.sixsq.slipstream.ssclj.resources.spec.user-identifier :as ui-spec]
    [com.sixsq.slipstream.ssclj.resources.user-identifier :as ui]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})


(deftest check-session-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str ui/resource-url "/hash-of-identifier")
             :resourceURI ui/resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl

             :identifier  "some-long-identifier"
             :user        {:href "user/abc"}}]

    (stu/is-valid ::ui-spec/user-identifier cfg)
    (stu/is-invalid ::ui-spec/user-identifier (assoc cfg :bad-attr "BAD_ATTR"))

    (doseq [attr #{:id :resourceURI :created :updated :acl :identifier :user}]
      (stu/is-invalid ::ui-spec/user-identifier (dissoc cfg attr)))

    (doseq [attr #{:username :server :clientIP}]
      (stu/is-valid ::ui-spec/user-identifier (dissoc cfg attr)))))
