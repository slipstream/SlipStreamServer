(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc-token-test
  (:require
    [clojure.test :refer [deftest is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc-token]))

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
             :group       "OIDC Token Authentication"
             :redirectURI "https://nuv.la/webui/profile"

             :token "{\"claim1\": \"value1\""}]

    (is (s/valid? :cimi/session-template.oidc-token cfg))
    (doseq [attr #{:id :resourceURI :created :updated :acl :method :instance :token}]
      (is (not (s/valid? :cimi/session-template.oidc-token (dissoc cfg attr)))))
    (doseq [attr #{:group :redirectURI}]
      (is (s/valid? :cimi/session-template.oidc-token (dissoc cfg attr))))))
