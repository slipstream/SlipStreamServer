(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-github-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-github :as session-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})


(deftest check-session-template-github-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str st/resource-url "/github")
             :resourceURI st/resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl

             :method      "github"
             :instance    "github"
             :group       "GitHub Authentication"
             :redirectURI "https://nuv.la/webui/profile"}]

    (stu/is-valid ::session-tpl/github cfg)

    (doseq [attr #{:id :resourceURI :created :updated :acl :method :instance}]
      (stu/is-invalid ::session-tpl/github (dissoc cfg attr)))

    (doseq [attr #{:group :redirectURI}]
      (stu/is-valid ::session-tpl/github (dissoc cfg attr)))))
