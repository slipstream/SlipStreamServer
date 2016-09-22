(ns com.sixsq.slipstream.auth.db-test
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

(deftest test-user-creation
  (is (= "st" (db/create-user! "github" "st" "st@s.com")))
  (let [users-created (kc/select db/users)]
    (is (= 1 (count users-created)))
    (is (= {:CYCLONELOGIN nil
            :GITHUBLOGIN  "st"
            :DELETED      false
            :EMAIL        "st@s.com"
            :ISSUPERUSER  false
            :ROLES        "alpha-role, beta-role"
            :JPAVERSION   0
            :NAME         "st"
            :RESOURCEURI  "user/st"
            :STATE        "ACTIVE"}
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
