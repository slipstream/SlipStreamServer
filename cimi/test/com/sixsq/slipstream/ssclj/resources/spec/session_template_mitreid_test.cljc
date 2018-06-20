(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-mitreid-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.session-template :as st]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-mitreid :as session-tpl]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest check-session-template-mitreid-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        cfg {:id          (str st/resource-url "/mitreid")
             :resourceURI st/resource-uri
             :created     timestamp
             :updated     timestamp
             :acl         valid-acl

             :method      "mitreid"
             :instance    "mitreid"
             :group       "MITREid Authentication"
             :redirectURI "https://nuv.la/webui/profile"}]

    (is (s/valid? ::session-tpl/mitreid cfg))
    (doseq [attr #{:id :resourceURI :created :updated :acl :method :instance}]
      (is (not (s/valid? ::session-tpl/mitreid (dissoc cfg attr)))))
    (doseq [attr #{:group :redirectURI}]
      (is (s/valid? ::session-tpl/mitreid (dissoc cfg attr))))))
