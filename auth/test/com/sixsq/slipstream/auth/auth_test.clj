(ns com.sixsq.slipstream.auth.auth-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.auth :as auth]
    [com.sixsq.slipstream.auth.test-helper :as th]))

(def valid-credentials  {:user-name "super"    :password   "supeRsupeR"})

(defn fixture-delete-all
  [f]
  (th/create-fake-empty-user-table)
  (f))

(use-fixtures :each fixture-delete-all)

(deftest test-auth-internal-invalid-credentials
  (th/add-user! valid-credentials)
  (is (= 401 (:status (auth/login {:params {:authn-method :internal}}))))
  (is (= 401 (:status (auth/login {:params {:authn-method :internal :user-name "super" :password "wrong"}})))))

(deftest test-auth-internal-valid-credentials
  (th/add-user! valid-credentials)

  (let [valid-request {:params {:authn-method :internal :user-name "super" :password "supeRsupeR"}}]
    (is (= 200 (:status (auth/login valid-request))))
    (is (not (nil? (get-in (auth/login valid-request) [:cookies "com.sixsq.slipstream.cookie" :value :token]))))))
