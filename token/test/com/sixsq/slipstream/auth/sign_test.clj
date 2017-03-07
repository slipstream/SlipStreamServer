(ns com.sixsq.slipstream.auth.sign-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [clj-time.coerce :as c]
    [com.sixsq.slipstream.auth.utils.timestamp :as t]
    [com.sixsq.slipstream.auth.sign :as s])
  (:import (clojure.lang ExceptionInfo)))

(deftest roundtrip-claims
  (let [timestamp (t/expiry-later)
        claims {:alpha "alpha"
                :beta  2
                :gamma 3.0
                :delta true
                :exp   timestamp}
        claims-expired (assoc claims :exp (t/expiry-now))]
    (is (= claims
           (-> claims s/sign-claims s/unsign-claims)))
    (Thread/sleep 2000)
    (is (thrown-with-msg? ExceptionInfo #"Token is expired"
                          (-> claims-expired s/sign-claims s/unsign-claims)))))
