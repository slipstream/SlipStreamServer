(ns com.sixsq.slipstream.auth.external-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.external :refer :all]
    [com.sixsq.slipstream.auth.test-helper :as th]))

(use-fixtures :each th/ssclj-server-fixture)

(deftest match-new-github-user-github
  (is (= [] (db/get-all-users)))
  (match-external-user! :github "st" "st@sixsq.com")
  (let [created-user (first (db/get-active-users))]
    (is (= "st" (:githublogin created-user)))
    (is (nil? (:cyclonelogin created-user)))
    (is (= "st@sixsq.com" (:emailAddress created-user)))
    (is (not (:deleted created-user)))
    (is (not (:isSuperUser created-user)))
    (is (= "" (:roles created-user)))
    (is (= "" (:organization created-user)))
    (is (= "st" (:username created-user)))
    (is (= "user/st" (:id created-user)))
    (is (= "ACTIVE" (:state created-user)))
    (is (= "auto" (:method created-user)))))

(deftest match-new-cyclone-user-github
  (is (= [] (db/get-all-users)))
  (match-external-user! :cyclone "st" "st@sixsq.com")
  (let [created-user (first (db/get-all-users))]
    (is (not (:deleted created-user)))
    (is (= "st@sixsq.com" (:emailAddress created-user)))
    (is (= "st" (:cyclonelogin created-user)))
    (is (nil? (:githublogin created-user)))
    (is (not (:isSuperUser created-user)))
    (is (= "" (:roles created-user)))
    (is (= "" (:organization created-user)))
    (is (= "st" (:username created-user)))
    (is (= "user/st" (:id created-user)))
    (is (= "ACTIVE" (:state created-user)))
    (is (= "auto" (:method created-user)))))

(deftest match-existing-user
  (th/add-user-for-test! {:username     "joe"
                          :password     "secret"
                          :emailAddress "st@sixsq.com"
                          :state        "ACTIVE"})
  (let [users-before-match (db/get-all-users)]
    (is (= 1 (count users-before-match)))
    (is (nil? (:githublogin (first users-before-match)))))
  (match-external-user! :github "st" "st@sixsq.com")
  (let [users-after-match (db/get-all-users)]
    (is (= 1 (count users-after-match)))
    (is (= "st" (:githublogin (first users-after-match))))))

(deftest match-already-mapped
  (let [user-info {:username     "joe"
                   :password     "secret"
                   :githublogin  "st"
                   :emailAddress "st@sixsq.com"
                   :state        "ACTIVE"}
        _         (th/add-user-for-test! user-info)
        user      (-> (db/get-all-users)
                      first
                      (dissoc :updated))]

    (match-external-user! :github "st" "st@sixsq.com")
    (is (= user (dissoc (first (db/get-all-users)) :updated)))

    (match-external-user! :cyclone "st" "st@sixsq.com")
    (is (= (assoc user :cyclonelogin "st")
           (dissoc (first (db/get-all-users)) :updated)))))

(deftest match-existing-deleted-user
  (th/add-user-for-test! {:username     "st"
                          :password     "secret"
                          :emailAddress "st@sixsq.com"
                          :state        "DELETED"})
  (let [users-before-match (db/get-all-users)]
    (is (= 1 (count users-before-match))))
  (match-external-user! :github "st" "st@sixsq.com")
  (let [users-after-match (db/get-all-users)
        new-user          (second users-after-match)]
    (is (= 2 (count users-after-match)))
    (is (= "st" (:githublogin new-user)))
    (is (= "st_1" (:username new-user)))))

(deftest oidc-user-names
  (let [users (db/get-active-users)]
    (is (zero? (count users))))
  (is (= "a_b_c_d_e" (create-user-when-missing! {:authn-login "a/b!c@d#e"
                                                 :email       "bad-address@example.com"})))
  (let [users (db/get-active-users)]
    (is (= 1 (count users))))
  (is (= "a_b_c_d_e" (create-user-when-missing! {:authn-login "a/b!c@d#e"
                                                 :email       "bad-address@example.com"})))
  (let [users (db/get-active-users)]
    (is (= 1 (count users))))
  (is (= "A_B_C_D_E" (create-user-when-missing! {:authn-login "A/B!C@D#E"
                                                 :email       "bad-address@example.com"})))
  (let [users (db/get-active-users)]
    (is (= 2 (count users)))))

(deftest check-create-user-when-missing!
  (let [users (db/get-active-users)]
    (is (zero? (count users))))
  (th/add-user-for-test! {:username     "not-missing"
                          :password     "secret"
                          :emailAddress "not-missing@example.com"
                          :state        "ACTIVE"})
  (let [users (db/get-all-users)]
    (is (= 1 (count users))))
  (is (= "not-missing" (create-user-when-missing! {:authn-login "not-missing"
                                                   :email       "bad-address@example.com"})))
  (let [users (db/get-all-users)]
    (is (= 1 (count users))))
  (is (= "missing" (create-user-when-missing! {:authn-login "missing"
                                               :email       "ok@example.com"})))
  (let [users (db/get-all-users)]
    (is (= 2 (count users))))
  (is (= "deleted" (create-user-when-missing! {:authn-login "deleted"
                                               :email       "ok@example.com"
                                               :state       "DELETED"})))
  (let [users (db/get-all-users)]
    (is (= 3 (count users))))
  (is (nil? (create-user-when-missing! {:authn-login       "deleted"
                                        :email             "ok@example.com"
                                        :fail-on-existing? true})))
  (let [users (db/get-all-users)]
    (is (= 3 (count users)))))

(deftest test-sanitize-login-name
  (is (= "st" (sanitize-login-name "st")))
  (is (= "Paul_Newman" (sanitize-login-name "Paul Newman")))
  (is (= "abc-def-123" (sanitize-login-name "abc-def-123")))
  (is (= "a_b_c_d_e" (sanitize-login-name "a/b!c@d#e"))))
