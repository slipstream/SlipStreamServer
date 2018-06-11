(ns com.sixsq.slipstream.ssclj.resources.spec.user-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.user :refer :all]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})


(deftest check-user-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        uname "120720737412@eduid.chhttps://eduid.ch/idp/shibboleth!https://fed-id.nuv.la/samlbridge/module.php/saml/sp/metadata.php/sixsq-saml-bridge!iqqrh4oiyshzcw9o40cvo0+pgka="
        cfg {:id               (str resource-url "/" uname)
             :resourceURI      resource-uri
             :created          timestamp
             :updated          timestamp
             :acl              valid-acl

             :username         uname
             :emailAddress     "me@example.com"

             :firstName        "John"
             :lastName         "Smith"
             :organization     "MyOrganization"
             :method           "direct"
             :href             "user-template/direct"
             :password         "hashed-password"
             :roles            "alpha,beta,gamma"
             :isSuperUser      false
             :state            "ACTIVE"
             :deleted          false
             :creation         timestamp
             :lastOnline       timestamp
             :lastExecute      timestamp
             :activeSince      timestamp
             :githublogin      "github-login"
             :cyclonelogin     "cyclone-login"
             :externalIdentity ["github:aGithubLogin"]
             :name             "me@example.com"}]

    (is (s/valid? :cimi/user cfg))
    (is (s/valid? :cimi/user (assoc cfg :externalIdentity nil)))
    (is (s/valid? :cimi/user (update cfg :externalIdentity conj "oidc:aOidcLogin")))
    (is (not (s/valid? :cimi/user (assoc cfg :unknown "value"))))
    (doseq [attr #{:id :resourceURI :created :updated :acl :username :emailAddress}]
      (is (not (s/valid? :cimi/user (dissoc cfg attr)))))
    (doseq [attr #{:firstName :lastName :organization :method :href :password
                   :roles :isSuperUser :state :deleted :creation :lastOnline
                   :lastExecute :activeSince :githublogin :cyclonelogin :externalIdentity :name}]
      (is (s/valid? :cimi/user (dissoc cfg attr))))))
