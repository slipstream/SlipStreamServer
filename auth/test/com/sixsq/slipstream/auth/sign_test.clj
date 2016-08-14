(ns com.sixsq.slipstream.auth.sign-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [buddy.sign.util :as util]
    [com.sixsq.slipstream.auth.sign :as t]))

(deftest roundtrip-claims
  (let [exp (t/expiry-timestamp)
        timestamp (util/to-timestamp exp)
        claims {:alpha "alpha"
                :beta  2
                :gamma 3.0
                :delta true
                :exp   exp}]
    (is (= (merge claims {:exp timestamp})
           (-> (t/sign-claims claims)
               (t/unsign-claims))))))
