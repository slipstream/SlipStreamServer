(ns com.sixsq.slipstream.metering-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.metering :as t]))

(deftest check-str->int
  (are [expected arg] (= expected (t/str->int arg))
                      nil nil
                      {:a :b} {:a :b}
                      "bad" "bad"
                      "123bad" "123bad"
                      1 1
                      1 "1"
                      1000 1000
                      -10 -10))

