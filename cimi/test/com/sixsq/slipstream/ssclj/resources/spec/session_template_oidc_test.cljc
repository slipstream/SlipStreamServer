(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc :as session-tpl]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest check-session-template-oidc-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str st/resource-url "/oidc")
             :resourceURI st/resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl

             :method      "oidc"
             :instance    "oidc"
             :group       "OIDC Authentication"
             :redirectURI "https://nuv.la/webui/profile"}]

    (is (s/valid? ::session-tpl/oidc cfg))
    (doseq [attr #{:id :resourceURI :created :updated :acl :method :instance}]
      (is (not (s/valid? ::session-tpl/oidc (dissoc cfg attr)))))
    (doseq [attr #{:group :redirectURI}]
      (is (s/valid? ::session-tpl/oidc (dissoc cfg attr))))))
