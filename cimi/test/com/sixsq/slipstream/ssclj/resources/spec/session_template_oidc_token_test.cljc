(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc-token-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc-token :as session-tpl]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})


(deftest check-session-template-oidc-token-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str st/resource-url "/oidc-token")
             :resourceURI st/resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl

             :method      "oidc-token"
             :instance    "oidc-token"
             :group       "Federated Identity"
             :redirectURI "https://nuv.la/webui/profile"

             :token       "some-compressed-oidc-token-value"}]

    (stu/is-valid ::session-tpl/oidc-token cfg)

    (doseq [attr #{:id :resourceURI :created :updated :acl :method :instance :token}]
      (stu/is-invalid ::session-tpl/oidc-token (dissoc cfg attr)))

    (doseq [attr #{:group :redirectURI}]
      (stu/is-valid ::session-tpl/oidc-token (dissoc cfg attr)))))
