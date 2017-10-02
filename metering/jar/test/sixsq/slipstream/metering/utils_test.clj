(ns sixsq.slipstream.metering.utils-test
  (:require
    [clojure.test :refer [deftest is are]]
    [sixsq.slipstream.metering.utils :as t]))

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

(deftest check-random-uuid
  (let [id-1 (t/random-uuid)
        id-2 (t/random-uuid)]
    (is (string? id-1))
    (is (string? id-2))
    (is (not= id-1 id-2))))
