(ns com.sixsq.slipstream.metering-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.metering :as t]
    [clj-time.core :as time]
    [clojure.core.async :as async]
    [clojure.data.json :as json]
    [clojure.java.io :as io]))

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

(deftest check-assoc-snapshot-timestamp
  (let [doc (-> (io/resource "virtual-machines.json")
                slurp
                (json/read-str :key-fn keyword))
        timestamp (time/now)
        results (t/assoc-snapshot-timestamp timestamp doc)]
    (is (= 10 (count results)))
    (doseq [result results]
      (is (= timestamp (:snapshot-time result))))))
