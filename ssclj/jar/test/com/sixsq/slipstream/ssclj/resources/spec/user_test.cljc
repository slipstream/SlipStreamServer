(ns com.sixsq.slipstream.ssclj.resources.spec.user-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.ssclj.resources.user :refer :all]
    [com.sixsq.slipstream.ssclj.resources.spec.user]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:type      "ROLE",
                         :principal "ADMIN",
                         :right     "ALL"}]})

(deftest check-user-schema
  (let [timestamp "1964-08-25T10:00:00.0Z"
        uname "120720737412@eduid.chhttps://eduid.ch/idp/shibboleth!https://fed-id.nuv.la/samlbridge/module.php/saml/sp/metadata.php/sixsq-saml-bridge!iqqrh4oiyshzcw9o40cvo0+pgka="
        cfg {:id           (str resource-url "/" uname)
             :resourceURI  resource-uri
             :created      timestamp
             :updated      timestamp
             :acl          valid-acl
             :username     uname
             :emailAddress "me@example.com"
             :firstName    "John"
             :lastName     "Smith"}]

    (is (s/valid? :cimi/user cfg))
    (is (not (s/valid? :cimi/user (assoc cfg :unknown "value"))))
    (doseq [attr #{:id :resourceURI :created :updated :acl :username :emailAddress}]
      (is (not (s/valid? :cimi/user (dissoc cfg attr)))))
    (doseq [attr #{:firstName :lastName}]
      (is (s/valid? :cimi/user (dissoc cfg attr))))))
