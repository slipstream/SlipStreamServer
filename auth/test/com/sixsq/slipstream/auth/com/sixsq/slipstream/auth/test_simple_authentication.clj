(ns com.sixsq.slipstream.auth.com.sixsq.slipstream.auth.test-simple-authentication
  (:refer-clojure :exclude [update])
  (:require
    [korma.core                                       :as kc]
    [clojure.test                                     :refer :all]
    [com.sixsq.slipstream.auth.simple-authentication  :as sa]
    [com.sixsq.slipstream.auth.core                   :as c]))

(defn fixture-delete-all
  [f]
  (sa/init)
  (kc/delete sa/users)
  (f))

(use-fixtures :each fixture-delete-all)

(def sa (sa/get-instance))

(def valid-credentials  {:user-name "joe"    :password   "secret"})

(def wrong-password     {:user-name "joe"    :password   "wrong"})
(def wrong-user         {:user-name "wrong"  :password   "secret"})
(def wrong-both         {:user-name "wrong"  :password   "wrong"})
(def missing-user       {:password  "secret"})
(def missing-password   {:user-name "joe"})
(def missing-both       {})

(def wrongs [wrong-user
             wrong-password
             wrong-both
             missing-user
             missing-password
             missing-both])

(defn- rejected?
  [[ok? result]]
  (= [ok? result] [false {:message "Invalid username or password"}]))

(defn- accepted?
  [[ok? result]]
  (and
    ok?
    (= (select-keys valid-credentials [:user-name]) result)
    (not (contains? result :password))))

(deftest test-auth-user-when-not-added
  (doseq [wrong (cons valid-credentials wrongs)]
    (is (rejected? (c/auth-user sa wrong)))))

(deftest test-auth-user
  (c/add-user! sa valid-credentials)
  (is (accepted? (c/auth-user sa valid-credentials)))
  (doseq [wrong wrongs]
    (is (rejected? (c/auth-user sa wrong)))))

(deftest test-create-token
  (c/add-user! sa valid-credentials)

  (let [[ok? token]
        (c/token sa valid-credentials)]
    (is ok?)
    (is (map? token))
    (is (contains? token :token)))

  (doseq [wrong wrongs]
    (is (rejected? (c/token sa wrong)))))

(deftest test-check-token-when-invalid-token
  (c/add-user! sa valid-credentials)
  (is (thrown? Exception (c/check-token sa "invalid token"))))

(deftest test-check-token-when-valid-token-retrieves-claims
  (c/add-user! sa valid-credentials)
  (let [valid-token (-> (c/token sa valid-credentials)
                        second
                        :token)]
    (is (= "joe" (:user-name (c/check-token sa valid-token))))))

;; TODO add tests to detect token obsolete after expiration

