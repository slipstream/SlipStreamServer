(ns com.sixsq.slipstream.auth.test-simple-authentication
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]
    [com.sixsq.slipstream.auth.database.ddl :as ddl]
    [com.sixsq.slipstream.auth.simple-authentication :as sa]
    [com.sixsq.slipstream.auth.core :as c]))

(defonce ^:private columns-users (ddl/columns "NAME"       "VARCHAR(100)"
                                              "PASSWORD"   "VARCHAR(200)"))

(defn fixture-delete-all
  [f]
  (sa/init)
  (ddl/create-table! "USER" columns-users)
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

(defn- token-rejected?
  [[ok? result]]
  (= [false "Invalid token"] [ok? (:message result)]))

(defn- accepted?
  [[ok? result]]
  (and
    ok?
    (= (select-keys valid-credentials [:user-name]) result)
    (not (contains? result :password))))

(deftest test-auth-user-when-not-added
  (doseq [wrong (cons valid-credentials wrongs)]
    (is (auth-rejected? (c/auth-user sa wrong)))))

(deftest test-auth-user
  (c/add-user! sa valid-credentials)
  (is (accepted? (c/auth-user sa valid-credentials)))

  (doseq [wrong wrongs]
    (is (auth-rejected? (c/auth-user sa wrong)))))

(deftest test-create-token
  (c/add-user! sa valid-credentials)

  (let [[ok? token]
        (c/token sa valid-credentials)]
    (is ok?)
    (is (map? token))
    (is (contains? token :token)))

  (doseq [wrong wrongs]
    (is (token-rejected? (c/token sa wrong)))))

(deftest test-check-token-when-invalid-token
  (c/add-user! sa valid-credentials)
  (is (thrown? Exception (c/check-token sa "invalid token"))))

(deftest test-check-token-when-valid-token-retrieves-claims
  (c/add-user! sa valid-credentials)
  (let [valid-token (-> (c/token sa valid-credentials)
                        second
                        :token)]
    (is (= "super" (:com.sixsq.identifier (c/check-token sa valid-token))))))

(deftest password-encryption-compatible-with-slipstream
  (is (= "304D73B9607B5DFD48EAC663544F8363B8A03CAAD6ACE21B369771E3A0744AAD0773640402261BD5F5C7427EF34CC76A2626817253C94D3B03C5C41D88C64399"
         (sa/sha512 "supeRsupeR"))))

;; TODO add tests to detect token obsolete after expiration
