(ns com.sixsq.slipstream.auth.utils.db-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.internal :as ia]
    [com.sixsq.slipstream.auth.test-helper :as th]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))

(use-fixtures :each ltu/with-test-server-fixture)

(deftest test-user-creation
  (is (= "st" (db/create-user! {:authn-method "github"
                                :authn-login  "st"
                                :email        "st@s.com"
                                :roles        "alpha-role, beta-role"
                                :firstname    "first"
                                :lastname     "last"
                                :organization "myorg"})))
  (let [user-names (db/existing-user-names)
        user       (db/get-user (first user-names))]
    (is (= 1 (count user-names)))
    (is (= "alpha-role, beta-role" (:roles user)))
    (is (= false (:deleted user)))
    (is (= "st@s.com" (:emailAddress user)))
    (is (= false (:isSuperUser user)))
    (is (= "st" (:username user)))
    (is (= "ACTIVE" (:state user)))
    (is (= "first" (:firstName user)))
    (is (= "last" (:lastName user)))
    (is (:password user))
    (is (:created user))
    (is (= "USER ANON alpha-role beta-role" (db/find-roles-for-username "st")))))

(deftest test-user-creation-avoids-user-same-name
  (th/add-user-for-test! {:username     "stef"
                          :password     "secret"
                          :emailAddress "st@s.com"})
  (is (= "stef_1" (db/create-user! "github" "stef" "st@s.com")))
  (let [users-created (db/get-active-users)]
    (is (= 2 (count users-created))))
  (is (nil? (db/create-user! {:authn-login       "stef"
                              :password          "secret"
                              :email             "st@s.com"
                              :fail-on-existing? true})))
  (let [users-created (db/get-active-users)]
    (is (= 2 (count users-created))))
  (is (= "stef_2" (db/create-user! {:authn-login       "stef"
                                    :password          "secret"
                                    :email             "st@s.com"
                                    :fail-on-existing? false})))
  (let [users-created (db/get-active-users)]
    (is (= 3 (count users-created)))))

(defn create-users
  [n]
  (doseq [i (range n)]
    (let [name (str "foo_" i)
          user {:id           (str "user/" name)
                :username     name
                :password     "12345"
                :emailAddress "a@b.c"}]
      (th/add-user-for-test! user))))

(deftest test-existing-user-names
  (is (= [] (db/existing-user-names)))
  (create-users 3)
  (is (= 3 (count (db/existing-user-names)))))

(deftest test-name-no-collision
  (is (= "_" (db/name-no-collision "_" [])))
  (is (= "_1" (db/name-no-collision "_" ["_"])))
  (is (= "" (db/name-no-collision "" [])))
  (is (= "_1" (db/name-no-collision "" [""])))

  (is (= ["joe", "joe_1", "joe_2"]
         (reduce #(conj %1 (db/name-no-collision %2 %1)) [] (repeat 3 "joe"))))

  (is (= "joe_" (db/name-no-collision "joe_" ["joe", "joe_1"])))
  (is (= "joe_1" (db/name-no-collision "joe_" ["joe", "joe_"])))
  (is (= "joe_2" (db/name-no-collision "joe_" ["joe", "joe_", "joe_1"])))
  (is (= "joe_11" (db/name-no-collision "joe_10" ["joe_10"])))
  (is (= "joe_1_2_4" (db/name-no-collision "joe_1_2_3" ["joe_1_2_3"]))))

(deftest test-build-roles
  (are [x super? roles] (= x (db/build-roles super? roles))
                        "ADMIN USER ANON" true nil
                        "USER ANON" false nil
                        "ADMIN USER ANON" true ""
                        "USER ANON" false ""
                        "ADMIN USER ANON" true " , , "
                        "USER ANON" false " , , "
                        "ADMIN USER ANON a" true "a"
                        "USER ANON a" false "a"
                        "ADMIN USER ANON a b" true ", a, ,  ,  b,  ,"
                        "USER ANON a b" false ", a, ,  ,  b,  ,"))

(deftest test-users-by-email-skips-deleted
  (th/add-user-for-test! {:username     "jack"
                          :password     "123456"
                          :emailAddress "jack@sixsq.com"
                          :state        "DELETED"})

  (is (= [] (db/find-usernames-by-email "unknown@xxx.com")))
  (is (= [] (db/find-usernames-by-email "jack@sixsq.com"))))

(deftest test-users-by-email
  (th/add-user-for-test! {:username     "jack"
                          :password     "123456"
                          :emailAddress "jack@sixsq.com"})
  (th/add-user-for-test! {:username     "joe"
                          :password     "123456"
                          :emailAddress "joe@sixsq.com"})
  (th/add-user-for-test! {:username     "joe-alias"
                          :password     "123456"
                          :emailAddress "joe@sixsq.com"})

  (is (= [] (db/find-usernames-by-email "unknown@xxx.com")))
  (is (= ["jack"] (db/find-usernames-by-email "jack@sixsq.com")))
  (is (= ["joe" "joe-alias"] (db/find-usernames-by-email "joe@sixsq.com"))))

(deftest test-users-by-authn-skips-deleted
  (th/add-user-for-test! {:username     "joe-slipstream"
                          :password     "123456"
                          :emailAddress "joe@sixsq.com"
                          :githublogin  "joe"
                          :state        "DELETED"})
  (is (nil? (db/find-username-by-authn :githublogin "joe"))))

(deftest test-users-by-authn
  (th/add-user-for-test! {:username     "joe-slipstream"
                          :password     "123456"
                          :emailAddress "joe@sixsq.com"
                          :githublogin  "joe"})

  (th/add-user-for-test! {:username     "jack-slipstream"
                          :password     "123456"
                          :emailAddress "jack@sixsq.com"
                          :githublogin  "jack"})

  (th/add-user-for-test! {:username     "alice-slipstream"
                          :password     "123456"
                          :emailAddress "alice@sixsq.com"})

  (is (nil? (db/find-username-by-authn :githublogin "unknownid")))
  (is (= "joe-slipstream" (db/find-username-by-authn :githublogin "joe"))))

(deftest test-users-by-authn-detect-inconsistent-data
  (th/add-user-for-test! {:username     "joe1-slipstream"
                          :password     "123456"
                          :emailAddress "jane@example.org"
                          :firstName    "Jane"
                          :lastName     "Tester"
                          :githublogin  "joe"})
  (th/add-user-for-test! {:username     "joe2-slipstream"
                          :password     "123456"
                          :emailAddress "jane@example.org"
                          :firstName    "Jane"
                          :lastName     "Tester"
                          :githublogin  "joe"})
  (is (thrown-with-msg? Exception #"one result for joe"
                        (db/find-username-by-authn :githublogin "joe"))))

(deftest check-user-exists?
  (let [test-username         "some-long-random-user-name-that-does-not-exist"
        test-username-deleted (str test-username "-deleted")]
    (is (false? (db/user-exists? test-username)))
    (is (false? (db/user-exists? test-username-deleted)))
    (th/add-user-for-test! {:username     test-username
                            :password     "password"
                            :emailAddress "jane@example.org"
                            :firstName    "Jane"
                            :lastName     "Tester"
                            :state        "ACTIVE"})
    (th/add-user-for-test! {:username     test-username-deleted
                            :password     "password"
                            :emailAddress "jane@example.org"
                            :firstName    "Jane"
                            :lastName     "Tester"
                            :state        "DELETED"})
    (is (true? (db/user-exists? test-username)))
    (is (false? (db/user-exists? test-username-deleted)))))

(deftest test-find-password-for-username
  (let [username  "testuser"
        password  "password"
        pass-hash (ia/hash-password password)
        user      {:username username
                   :password password}]
    (th/add-user-for-test! user)
    (is (= pass-hash (db/find-password-for-username username)))))

(deftest test-find-roles-for-username
  (let [username "testuser"
        user     {:username    username
                  :password    "password"
                  :isSuperUser false
                  :roles       "alpha-role, beta-role"}]
    (th/add-user-for-test! user)
    (is (= "USER ANON alpha-role beta-role" (db/find-roles-for-username username))))

  ; FIXME: requires direct user creation by super to be able to set isSuperUser to true
  #_(let [username "super"
          user     {:username    username
                    :password    "password"
                    :isSuperUser true
                    :roles       "alpha-role, beta-role"}]
      (th/add-user-for-test! user)
      (is (= "ADMIN USER ANON alpha-role beta-role" (db/find-roles-for-username username)))))
