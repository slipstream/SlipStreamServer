(ns com.sixsq.slipstream.auth.internal-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.test-helper :as th]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.internal :as ia]
    [com.sixsq.slipstream.auth.sign :as sg]))

(defn fixture-delete-all
  [f]
  (th/create-test-empty-user-table)
  (f))

(use-fixtures :each fixture-delete-all)

(defn- damage [creds key] (assoc creds key "WRONG"))

(def valid-credentials {:username "super" :password "supeRsupeR"})
(def wrong-password (damage valid-credentials :password))
(def wrong-user (damage valid-credentials :username))
(def wrong-both (-> valid-credentials
                    (damage :username)
                    (damage :password)))
(def missing-user (dissoc valid-credentials :username))
(def missing-password (dissoc valid-credentials :password))
(def missing-both {})

(def wrongs [wrong-user
             wrong-password
             wrong-both
             missing-user
             missing-password
             missing-both])

(defn- token-refused?
  [[ok? result]]
  (= [false "Invalid credentials when creating token"] [ok? (:message result)]))

(defn- token-created?
  [[ok? token]]
  (is ok?)
  (is (map? token))
  (is (contains? token :token)))

(defn token-value
  [[_ token]]
  (:token token))

(deftest all-rejected-when-no-user-added
  (doseq [wrong (cons valid-credentials wrongs)]
    (is (not (ia/valid? wrong))))
  (is (not (ia/valid? valid-credentials))))

(deftest test-valid-credentials
  (th/add-user-for-test! valid-credentials)

  (is (ia/valid? valid-credentials))
  (doseq [wrong wrongs]
    (is (not (ia/valid? wrong)))))

(deftest test-check-token-when-invalid-token
  (th/add-user-for-test! valid-credentials)
  (is (thrown? Exception (sg/unsign-claims {:token "invalid token"}))))

(deftest password-encryption-compatible-with-slipstream
  (is (= "304D73B9607B5DFD48EAC663544F8363B8A03CAAD6ACE21B369771E3A0744AAD0773640402261BD5F5C7427EF34CC76A2626817253C94D3B03C5C41D88C64399"
         (ia/sha512 "supeRsupeR"))))

(deftest test-users-by-email-skips-deleted
  (th/add-user-for-test! {:username "jack"
                          :password  "123456"
                          :email     "jack@sixsq.com"
                          :state     "DELETED"})

  (is (= [] (db/find-usernames-by-email "unknown@xxx.com")))
  (is (= [] (db/find-usernames-by-email "jack@sixsq.com"))))

(deftest test-users-by-email
  (th/add-user-for-test! {:username "jack"
                          :password  "123456"
                          :email     "jack@sixsq.com"})
  (th/add-user-for-test! {:username "joe"
                          :password  "123456"
                          :email     "joe@sixsq.com"})
  (th/add-user-for-test! {:username "joe-alias"
                          :password  "123456"
                          :email     "joe@sixsq.com"})

  (is (= [] (db/find-usernames-by-email "unknown@xxx.com")))
  (is (= ["jack"] (db/find-usernames-by-email "jack@sixsq.com")))
  (is (= ["joe" "joe-alias"] (db/find-usernames-by-email "joe@sixsq.com"))))

(deftest test-users-by-authn-skips-deleted
  (th/add-user-for-test! {:username "joe-slipstream"
                          :password  "123456"
                          :email     "joe@sixsq.com"
                          :github-id "joe"
                          :state     "DELETED"})
  (is (nil? (db/find-username-by-authn :github "joe"))))

(deftest test-users-by-authn
  (th/add-user-for-test! {:username "joe-slipstream"
                          :password  "123456"
                          :email     "joe@sixsq.com"
                          :github-id "joe"})

  (th/add-user-for-test! {:username "jack-slipstream"
                          :password  "123456"
                          :email     "jack@sixsq.com"
                          :github-id "jack"})

  (th/add-user-for-test! {:username "alice-slipstream"
                          :password  "123456"
                          :email     "alice@sixsq.com"})

  (is (nil? (db/find-username-by-authn :github "unknownid")))
  (is (= "joe-slipstream" (db/find-username-by-authn :github "joe"))))

(deftest test-users-by-authn-detect-inconsistent-data
  (dotimes [_ 2] (th/add-user-for-test! {:username "joe-slipstream"
                                         :password  "123456"
                                         :email     "joe@sixsq.com"
                                         :github-id "joe"}))
  (is (thrown-with-msg? Exception #"one result for joe" (db/find-username-by-authn :github "joe"))))
