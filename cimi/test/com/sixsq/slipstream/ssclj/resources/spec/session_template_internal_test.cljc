(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-internal-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal :as session-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})


(deftest check-session-template-internal-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str st/resource-url "/internal")
             :resourceURI st/resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl

             :method      "internal"
             :instance    "internal"
             :group       "Federated Identity"
             :redirectURI "https://nuv.la/webui/profile"

             :username    "user"
             :password    "pass"}]

    (stu/is-valid ::session-tpl/internal cfg)

    (doseq [attr #{:id :resourceURI :created :updated :acl :method :instance :username :password}]
      (stu/is-invalid ::session-tpl/internal (dissoc cfg attr)))

    (doseq [attr #{:group :redirectURI}]
      (stu/is-valid ::session-tpl/internal (dissoc cfg attr)))))
