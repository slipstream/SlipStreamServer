(ns com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.sixsq.slipstream.auth.test-helper :as th]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils :as uiu]))


(use-fixtures :each ltu/with-test-server-fixture)


(deftest create-user-identifier
  (let [name "some-username"
        user {:id           (str "user/" name)
              :username     name
              :password     "12345"
              :emailAddress "a@b.c"}
        authn-method :sample-method
        external-login "some-external-login"
        instance "some-instance"
        identifier (uiu/generate-identifier authn-method external-login instance)
        user-response (th/add-user-for-test! user)
        user-id (-> user-response :body :resource-id)
        user-identifier-response (uiu/add-user-identifier! name authn-method external-login instance)
        user-identifier-id (-> user-identifier-response :body :resource-id)]
    (is (= 201 (:status user-identifier-response)))
    (is (= (u/md5 identifier) (-> user-identifier-id (str/split #"/") second)))

    (let [resource (crud/retrieve-by-id user-identifier-id)]
      (is (= identifier (:identifier resource)))
      (is (= {:href user-id} (:user resource))))))


(deftest identities-for-user
  (let [name "some-username"
        external-login "some-external-login"
        user {:id           (str "user/" name)
              :username     name
              :password     "12345"
              :emailAddress "a@b.c"}
        _ (th/add-user-for-test! user)
        authn-methods #{:some-method :other-method :third-method}]

    (doseq [authn-method authn-methods]
      (uiu/add-user-identifier! name authn-method external-login nil))

    (let [results (uiu/find-identities-by-user (:id user))]
      (is (= (count authn-methods) (count results)))
      (is (= (map :identifier results) (map #(uiu/generate-identifier % external-login nil) authn-methods))))))


(deftest identities-for-user-with-instance
  (let [name "some-username"
        instance "some-instance"
        external-login "some-external-login"
        user {:id           (str "user/" name)
              :username     name
              :password     "12345"
              :emailAddress "a@b.c"}
        _ (th/add-user-for-test! user)
        authn-methods #{:some-method :other-method :third-method}]
    (doseq [authn-method authn-methods]
      (uiu/add-user-identifier! name authn-method external-login instance))

    (let [results (uiu/find-identities-by-user (:id user))]
      (is (= 1 (count results)))
      (is (= (set (map :identifier results)) (set (map #(uiu/generate-identifier % external-login instance) authn-methods)))))))


(deftest find-username-by-identifier
  (let [name "some-username"
        authn-method :some-method
        unsanitized-external-login "120720737412@eduid_chhttps#$$**eduid_ch_idp_shibboleth!https//fed-id.nuv.la]]samlbridge%module^php_*saml/sp\\metadata}php_sixsq}saml|bridge'iqqrh4oiyshzcw9o40cvo0;pgka_"
        sanitized-external-login (uiu/sanitize-login-name unsanitized-external-login)
        user {:id           (str "user/" name)
              :username     name
              :password     "12345"
              :emailAddress "a@b.c"}
        _ (th/add-user-for-test! user)
        instances #{"instance1" "instance2" "instance3"}]

    ;; Username should be found both sanitized and unsanitized login
    (doseq [instance instances]
      ;; Create User identifier with a sanitized identifier
      (uiu/add-user-identifier! name authn-method sanitized-external-login instance)
      (is (= name (uiu/find-username-by-identifier authn-method instance sanitized-external-login)))
      (is (= name (uiu/find-username-by-identifier authn-method instance unsanitized-external-login))))

    ;; Need some time for complete removal of the sanitized identifier
    (Thread/sleep 2000)

    (let [identifiers (uiu/find-identities-by-user (:id user))]
      (is (= (count instances) (count identifiers)))

      ;; Check that eventually the stored identifier is the unsanitized one
      (is (= unsanitized-external-login (->> identifiers
                                             (map :identifier)
                                             (map #(str/split % #":"))
                                             (map second)
                                             first))))))
