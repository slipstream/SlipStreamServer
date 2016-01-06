(ns com.sixsq.slipstream.auth.internal-authentication-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]
    [com.sixsq.slipstream.auth.database.ddl :as ddl]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.internal-authentication :as ia]
    [com.sixsq.slipstream.auth.sign :as sg]))

(defonce ^:private columns-users (ddl/columns "NAME"        "VARCHAR(100)"
                                              "PASSWORD"    "VARCHAR(200)"
                                              "EMAIL"       "VARCHAR(200)"
                                              "AUTHNMETHOD" "VARCHAR(200)"
                                              "AUTHNID"     "VARCHAR(200)"))

(defn fixture-delete-all
  [f]
  (db/init)
  (ddl/create-table! "USER" columns-users)
  (kc/delete db/users)
  (f))

(use-fixtures :each fixture-delete-all)

(defn- damage [creds key] (assoc creds key "WRONG"))

(def valid-credentials  {:user-name "super"    :password   "supeRsupeR"})
(def wrong-password     (damage valid-credentials :password))
(def wrong-user         (damage valid-credentials :user-name))
(def wrong-both         (-> valid-credentials
                            (damage :user-name)
                            (damage :password)))
(def missing-user       (dissoc valid-credentials :user-name))
(def missing-password   (dissoc valid-credentials :password))
(def missing-both       {})

(def wrongs [wrong-user
             wrong-password
             wrong-both
             missing-user
             missing-password
             missing-both])

(defn- auth-rejected?
  [[ok? result]]
  (and
    (false? ok?)
    (.startsWith (:message result) "Invalid combination username/password for" )))

(defn- token-refused?
  [[ok? result]]
  (= [false "Invalid credentials when creating token"] [ok? (:message result)]))

(defn- token-created?
  [[ok? token]]
  (is ok?)
  (is (map? token))
  (is (contains? token :token)))

(defn- auth-accepted?
  [[ok? result]]
  (and
    ok?
    (= (select-keys valid-credentials [:user-name]) result)
    (not (contains? result :password))))

(defn token-value
  [[_ token]]
  (:token token))

(deftest all-rejected-when-no-user-added
  (doseq [wrong (cons valid-credentials wrongs)]
    (is (auth-rejected? (ia/auth-user wrong)))))

(deftest test-auth-user
  (ia/add-user! valid-credentials)
  (is (auth-accepted? (ia/auth-user valid-credentials)))

  (doseq [wrong wrongs]
    (is (auth-rejected? (ia/auth-user wrong)))))

(deftest test-create-token
  (ia/add-user! valid-credentials)

  (is (token-created? (ia/create-token valid-credentials)))

  (doseq [wrong wrongs]
    (is (token-refused? (ia/create-token wrong)))))

(deftest test-check-token-when-invalid-token
  (ia/add-user! valid-credentials)
  (is (thrown? Exception (sg/check-token {:token "invalid token"}))))

(deftest test-check-token-when-valid-token-retrieves-claims
  (ia/add-user! valid-credentials)
  (let [valid-token (-> (ia/create-token valid-credentials)
                        token-value)]
    (is (= "super" (:com.sixsq.identifier (sg/check-token valid-token))))))

(deftest password-encryption-compatible-with-slipstream
  (is (= "304D73B9607B5DFD48EAC663544F8363B8A03CAAD6ACE21B369771E3A0744AAD0773640402261BD5F5C7427EF34CC76A2626817253C94D3B03C5C41D88C64399"
         (sg/sha512 "supeRsupeR"))))

(deftest check-claims-token
  (ia/add-user! valid-credentials)
  (let [claims {:a 1 :b 2}
        valid-token (-> (ia/create-token valid-credentials)
                        token-value)
        claim-token (-> (ia/create-token claims valid-token)
                        token-value)]
    (is (= claims (sg/check-token claim-token)))))

(deftest test-users-by-email
  (ia/add-user! {:user-name "jack"
                   :password  "123456"
                   :email     "jack@sixsq.com"})
  (ia/add-user! {:user-name "joe"
                   :password  "123456"
                   :email     "joe@sixsq.com"})
  (ia/add-user! {:user-name "joe-alias"
                   :password  "123456"
                   :email     "joe@sixsq.com"})

  (is (= []                   (db/find-usernames-by-email "unknown@xxx.com")))
  (is (= ["jack"]             (db/find-usernames-by-email "jack@sixsq.com")))
  (is (= ["joe" "joe-alias"]  (db/find-usernames-by-email "joe@sixsq.com"))))

(deftest test-users-by-authn
  (ia/add-user! {:user-name     "joe-slipstream"
                   :password      "123456"
                   :email         "joe@sixsq.com"
                   :authn-method  "github"
                   :authn-id      "joe"})

  (ia/add-user! {:user-name     "jack-slipstream"
                   :password      "123456"
                   :email         "jack@sixsq.com"
                   :authn-method  "github"
                   :authn-id      "jack"})

  (ia/add-user! {:user-name     "alice-slipstream"
                   :password      "123456"
                   :email         "alice@sixsq.com"})

  (is (nil? (db/find-username-by-authn "unknown-authn-method" "id")))
  (is (nil? (db/find-username-by-authn "github" "john")))
  (is (= "joe-slipstream" (db/find-username-by-authn "github" "joe"))))

(deftest test-users-by-authn-detect-inconsistent-data
  (dotimes [_ 2] (ia/add-user! {:user-name     "joe-slipstream"
                                  :password      "123456"
                                  :email         "joe@sixsq.com"
                                  :authn-method  "github"
                                  :authn-id      "joe"}))
  (is (thrown? Exception "joe-slipstream" (db/find-username-by-authn "github" "joe"))))
