(ns com.sixsq.slipstream.auth.test-simple-authentication
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]
    [com.sixsq.slipstream.auth.database.ddl :as ddl]
    [com.sixsq.slipstream.auth.simple-authentication :as sa]
    [com.sixsq.slipstream.auth.core :as c]))

(defonce ^:private columns-users (ddl/columns "name"       "VARCHAR(100)"
                                              "password"   "VARCHAR(200)"))

(defn fixture-delete-all
  [f]
  (sa/init)
  (ddl/create-table! "user_" columns-users)
  (kc/delete sa/users)
  (f))

(use-fixtures :each fixture-delete-all)

(def sa (sa/get-instance))

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
    (is (auth-rejected? (c/auth-user sa wrong)))))

(deftest test-auth-user
  (c/add-user! sa valid-credentials)
  (is (auth-accepted? (c/auth-user sa valid-credentials)))

  (doseq [wrong wrongs]
    (is (auth-rejected? (c/auth-user sa wrong)))))

(deftest test-create-token
  (c/add-user! sa valid-credentials)

  (is (token-created? (c/token sa valid-credentials)))

  (doseq [wrong wrongs]
    (is (token-refused? (c/token sa wrong)))))

(deftest test-check-token-when-invalid-token
  (c/add-user! sa valid-credentials)
  (is (thrown? Exception (c/check-token sa {:token "invalid token"}))))

(deftest test-check-token-when-valid-token-retrieves-claims
  (c/add-user! sa valid-credentials)
  (let [valid-token (-> (c/token sa valid-credentials)
                        token-value)]
    (is (= "super" (:com.sixsq.identifier (c/check-token sa valid-token))))))

(deftest password-encryption-compatible-with-slipstream
  (is (= "304D73B9607B5DFD48EAC663544F8363B8A03CAAD6ACE21B369771E3A0744AAD0773640402261BD5F5C7427EF34CC76A2626817253C94D3B03C5C41D88C64399"
         (sa/sha512 "supeRsupeR"))))

(comment
  ;; Test too fragile or slow
  (deftest token-invalid-after-expiry
    (c/add-user! sa valid-credentials)
    (let [[_ token-map] (c/token sa valid-credentials)]
      (c/check-token sa (:token token-map))
      (Thread/sleep 10000)
      (is (thrown? Exception (c/check-token sa (:token token-map)))))))

(deftest check-claims-token
  (c/add-user! sa valid-credentials)
  (let [claims {:a 1 :b 2}
        valid-token (-> (c/token sa valid-credentials)
                        token-value)
        claim-token (-> (c/token sa claims valid-token)
                        token-value)]
    (is (= claims (c/check-token sa claim-token)))))

