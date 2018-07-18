(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-mitreid-token-test
  (:require
    [clojure.test :refer [deftest]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-mitreid-token :as cfg-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-configuration-template-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          (str ct/resource-url "/session-mitreid-token-test-instance")
              :resourceURI p/service-context
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl

              :service     "session-mitreid-token"
              :instance    "test-instance"
              :clientIPs   ["127.0.0.1", "192.168.100.100"]}]

    (stu/is-valid ::cfg-tpl/session-mitreid-token root)

    (stu/is-valid ::cfg-tpl/session-mitreid-token (assoc root :clientIPs ["127.0.0.1"]))
    (stu/is-invalid ::cfg-tpl/session-mitreid-token (assoc root :clientIPs "127.0.0.1"))

    (doseq [k #{:id :resourceURI :created :updated :acl :service :instance}]
      (stu/is-invalid ::cfg-tpl/session-mitreid-token (dissoc root k)))

    (doseq [k #{:clientIPs}]
      (stu/is-valid ::cfg-tpl/session-mitreid-token (dissoc root k)))))
