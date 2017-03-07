(ns com.sixsq.slipstream.auth.auth-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.auth :as auth]
    [com.sixsq.slipstream.auth.test-helper :as th]))

(def valid-credentials {:username "super" :password "supeRsupeR"})
(def valid-request {:params (merge {:authn-method :internal} valid-credentials)})

(defn fixture-delete-all
  [f]
  (th/create-test-empty-user-table)
  (f))

(use-fixtures :each fixture-delete-all)

(defn login-status [request]
  (:status (auth/login request)))

(deftest test-internal-login
  (th/add-user-for-test! valid-credentials)
  (are [status request] (= status (login-status request))
                        200 valid-request
                        200 {:params {:authn-method :internal
                                      :username     "super"
                                      :password     "supeRsupeR"}}
                        200 {:params {:authn-method :internal
                                      :user-name    "super"
                                      :password     "supeRsupeR"}}
                        200 {:params {:authn-method :internal
                                      :username     "super" :user-name "wrong"
                                      :password     "supeRsupeR"}}
                        401 {:params {:authn-method :internal}}
                        401 {:params {:authn-method :internal
                                      :username     "super"
                                      :password     "wrong"}}
                        401 {:params {:authn-method :internal
                                      :username     "wrong"
                                      :user-name    "super"
                                      :password     "supeRsupeR"}})
  (is (get-in (auth/login valid-request) [:cookies "com.sixsq.slipstream.cookie" :value])))

(deftest test-auth-logout
  (let [logout-response (auth/logout valid-request)
        cookies (get-in logout-response [:cookies "com.sixsq.slipstream.cookie"])]
    (is (= 200 (:status logout-response)))
    (is (= "INVALID" (:value cookies)))
    (is (zero? (:max-age cookies)))))


