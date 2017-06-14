(ns com.sixsq.slipstream.auth.utils.db-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.test-helper :as th]))

(defn fixture-delete-all
  [f]
  (th/create-test-empty-user-table)
  (f))

(use-fixtures :each fixture-delete-all)

(deftest test-user-creation-legacy
  (is (= "st" (db/create-user! "github" "st" "st@s.com")))
  (let [users-created (kc/select db/users)]
    (is (= 1 (count users-created)))
    (is (= {:CYCLONELOGIN nil
            :GITHUBLOGIN  "st"
            :ROLES        nil
            :DELETED      false
            :EMAIL        "st@s.com"
            :ISSUPERUSER  false
            :JPAVERSION   0
            :NAME         "st"
            :RESOURCEURI  "user/st"
            :STATE        "ACTIVE"
            :FIRSTNAME    nil
            :LASTNAME     nil
            :ORGANIZATION nil}
           (-> users-created first (dissoc :CREATION :PASSWORD))))
    (is (-> users-created first :PASSWORD))
    (is (-> users-created first :CREATION))

    (is (= "USER ANON" (db/find-roles-for-username "st")))))

(deftest test-user-creation
  (is (= "st" (db/create-user! {:authn-method "github"
                                :authn-login  "st"
                                :email        "st@s.com"
                                :roles        "alpha-role, beta-role"
                                :firstname    "first"
                                :lastname     "last"
                                :organization "myorg"})))
  (let [users-created (kc/select db/users)]
    (is (= 1 (count users-created)))
    (is (= {:CYCLONELOGIN nil
            :GITHUBLOGIN  "st"
            :ROLES        "alpha-role, beta-role"
            :DELETED      false
            :EMAIL        "st@s.com"
            :ISSUPERUSER  false
            :JPAVERSION   0
            :NAME         "st"
            :RESOURCEURI  "user/st"
            :STATE        "ACTIVE"
            :FIRSTNAME    "first"
            :LASTNAME     "last"
            :ORGANIZATION "myorg"}
           (-> users-created first (dissoc :CREATION :PASSWORD))))
    (is (-> users-created first :PASSWORD))
    (is (-> users-created first :CREATION))

    (is (= "USER ANON alpha-role beta-role" (db/find-roles-for-username "st")))))

(deftest test-user-creation-avoids-user-same-name
  (th/add-user-for-test! {:username "stef" :password "secret"})
  (is (= "stef_1" (db/create-user! "github" "stef" "st@s.com")))
  (let [users-created (kc/select db/users)]
    (is (= 2 (count users-created)))))

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
  (th/add-user-for-test! {:username "jack"
                          :password "123456"
                          :email    "jack@sixsq.com"
                          :state    "DELETED"})

  (is (= [] (db/find-usernames-by-email "unknown@xxx.com")))
  (is (= [] (db/find-usernames-by-email "jack@sixsq.com"))))

(deftest test-users-by-email
  (th/add-user-for-test! {:username "jack"
                          :password "123456"
                          :email    "jack@sixsq.com"})
  (th/add-user-for-test! {:username "joe"
                          :password "123456"
                          :email    "joe@sixsq.com"})
  (th/add-user-for-test! {:username "joe-alias"
                          :password "123456"
                          :email    "joe@sixsq.com"})

  (is (= [] (db/find-usernames-by-email "unknown@xxx.com")))
  (is (= ["jack"] (db/find-usernames-by-email "jack@sixsq.com")))
  (is (= ["joe" "joe-alias"] (db/find-usernames-by-email "joe@sixsq.com"))))

(deftest test-users-by-authn-skips-deleted
  (th/add-user-for-test! {:username  "joe-slipstream"
                          :password  "123456"
                          :email     "joe@sixsq.com"
                          :github-id "joe"
                          :state     "DELETED"})
  (is (nil? (db/find-username-by-authn :github "joe"))))

(deftest test-users-by-authn
  (th/add-user-for-test! {:username  "joe-slipstream"
                          :password  "123456"
                          :email     "joe@sixsq.com"
                          :github-id "joe"})

  (th/add-user-for-test! {:username  "jack-slipstream"
                          :password  "123456"
                          :email     "jack@sixsq.com"
                          :github-id "jack"})

  (th/add-user-for-test! {:username "alice-slipstream"
                          :password "123456"
                          :email    "alice@sixsq.com"})

  (is (nil? (db/find-username-by-authn :github "unknownid")))
  (is (= "joe-slipstream" (db/find-username-by-authn :github "joe"))))

(deftest test-users-by-authn-detect-inconsistent-data
  (dotimes [_ 2] (th/add-user-for-test! {:username  "joe-slipstream"
                                         :password  "123456"
                                         :email     "joe@sixsq.com"
                                         :github-id "joe"}))
  (is (thrown-with-msg? Exception #"one result for joe" (db/find-username-by-authn :github "joe"))))

(deftest check-user-exists?
  (let [test-username "some-long-random-user-name-that-does-not-exist"
        test-username-deleted (str test-username "-deleted")]
    (is (false? (db/user-exists? test-username)))
    (is (false? (db/user-exists? test-username-deleted)))
    (th/add-user-for-test! {:username test-username :password "secret" :email "user@example.com" :state "ACTIVE"})
    (th/add-user-for-test! {:username test-username-deleted :password "secret" :email "user@example.com" :state "DELETED"})
    (is (true? (db/user-exists? test-username)))
    (is (false? (db/user-exists? test-username-deleted)))))

