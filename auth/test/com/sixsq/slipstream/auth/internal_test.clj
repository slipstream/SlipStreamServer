(ns com.sixsq.slipstream.auth.internal-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.internal :as t]
    [com.sixsq.slipstream.auth.test-helper :as th]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.sign :as sg]))

(defn fixture-delete-all
  [f]
  (th/create-test-empty-user-table)
  (f))

(use-fixtures :each fixture-delete-all)

(def valid-creds-super {:username "super" :password "supeRsupeR"})
(def valid-creds-jane {:username "jane" :password "tarzan"})

(def invalid-creds [{:username "WRONG" :password "supeRsupeR"}
                    {:username "super" :password "WRONG"}
                    {:username "WRONG" :password "WRONG"}
                    {:username "WRONG" :password "supeRsupeR"}
                    {:username "super"}
                    {}])

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

(deftest test-password-hashing
  (is (nil? (t/sha512 nil)))
  (is (= "304D73B9607B5DFD48EAC663544F8363B8A03CAAD6ACE21B369771E3A0744AAD0773640402261BD5F5C7427EF34CC76A2626817253C94D3B03C5C41D88C64399"
         (t/sha512 "supeRsupeR"))))

(deftest check-valid?
  (th/add-user-for-test! valid-creds-super)
  (th/add-user-for-test! valid-creds-jane)

  (is (t/valid? valid-creds-super))
  (is (t/valid? valid-creds-jane))
  (doseq [wrong invalid-creds]
    (is (not (t/valid? wrong)))))

(deftest check-valid?-no-users
  ;; no users added

  (is (not (t/valid? valid-creds-super)))
  (is (not (t/valid? valid-creds-jane)))
  (doseq [wrong invalid-creds]
    (is (not (t/valid? wrong)))))

(deftest check-create-claims
  (th/add-user-for-test! (merge valid-creds-super {:issuperuser true}))
  (th/add-user-for-test! (merge valid-creds-jane {:issuperuser false}))

  (is (= {:com.sixsq.identifier "jane"
          :com.sixsq.roles      "USER ANON"}
         (t/create-claims "jane")))
  (is (= {:com.sixsq.identifier "super"
          :com.sixsq.roles      "ADMIN USER ANON"}
         (t/create-claims "super"))))

(deftest check-login
  (th/add-user-for-test! (merge valid-creds-super {:issuperuser true}))
  (th/add-user-for-test! (merge valid-creds-jane {:issuperuser false}))

  (let [response (t/login {:params valid-creds-super})]
    (is (= 200 (:status response)))
    (is (get-in response [:cookies "com.sixsq.slipstream.cookie" :value])))

  ;; FIXME: This should really return 403.
  (let [response (t/login {:params {:username "super" :password "WRONG"}})]
    (is (= 401 (:status response)))
    (is (nil? (get-in response [:cookies "com.sixsq.slipstream.cookie" :value]))))

  (let [response (t/login {:params valid-creds-jane})]
    (is (= 200 (:status response)))
    (is (get-in response [:cookies "com.sixsq.slipstream.cookie" :value])))

  ;; FIXME: This should really return 403.
  (let [response (t/login {:params {:username "jane" :password "WRONG"}})]
    (is (= 401 (:status response)))
    (is (nil? (get-in response [:cookies "com.sixsq.slipstream.cookie" :value])))))

(deftest check-logout
  (let [response (t/logout)]
    (is (= 200 (:status response)))
    (is (= "INVALID" (get-in response [:cookies "com.sixsq.slipstream.cookie" :value])))))

#_(deftest test-create-token
    (th/add-user-for-test! valid-creds-super)

    (is (token-created? (t/create-token valid-creds-super)))
    (doseq [wrong invalid-creds]
      (is (token-refused? (t/create-token wrong)))))

#_(deftest test-check-token-when-invalid-token
    (th/add-user-for-test! valid-creds-super)
    (is (thrown? Exception (sg/unsign-claims {:token "invalid token"}))))

#_(deftest test-check-token-when-valid-token-retrieves-claims
    (th/add-user-for-test! valid-creds-super)
    (let [valid-token (token-value (t/create-token valid-creds-super))]
      (is (= "super" (:com.sixsq.identifier (sg/unsign-claims valid-token))))))

#_(deftest test-create-token-removes_password-from-token
    (th/add-user-for-test! valid-creds-super)
    (let [valid-token (token-value (t/create-token valid-creds-super))]
      (is (nil? (:password (sg/unsign-claims valid-token))))))

#_(deftest check-claims-token
    (th/add-user-for-test! valid-creds-super)
    (let [claims {:a 1 :b 2}
          valid-token (token-value (t/create-token valid-creds-super))
          claim-token (token-value (t/create-token claims valid-token))]
      (is (= claims (sg/unsign-claims claim-token)))))

#_(deftest test-users-by-email-skips-deleted
    (th/add-user-for-test! {:username "jack"
                            :password "123456"
                            :email    "jack@sixsq.com"
                            :state    "DELETED"})

    (is (= [] (db/find-usernames-by-email "unknown@xxx.com")))
    (is (= [] (db/find-usernames-by-email "jack@sixsq.com"))))

#_(deftest test-users-by-email
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

#_(deftest test-users-by-authn-skips-deleted
    (th/add-user-for-test! {:username  "joe-slipstream"
                            :password  "123456"
                            :email     "joe@sixsq.com"
                            :github-id "joe"
                            :state     "DELETED"})
    (is (nil? (db/find-username-by-authn :github "joe"))))

#_(deftest test-users-by-authn
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

#_(deftest test-users-by-authn-detect-inconsistent-data
    (dotimes [_ 2] (th/add-user-for-test! {:username  "joe-slipstream"
                                           :password  "123456"
                                           :email     "joe@sixsq.com"
                                           :github-id "joe"}))
    (is (thrown-with-msg? Exception #"one result for joe" (db/find-username-by-authn :github "joe"))))
