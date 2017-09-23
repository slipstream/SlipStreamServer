(ns com.sixsq.slipstream.metering-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.metering :as t]
    [clj-time.core :as time]
    [clojure.core.async :as async]
    [clojure.data.json :as json]
    [clojure.java.io :as io]))

(deftest check-assoc-snapshot-timestamp-xf
  (let [doc (-> (io/resource "virtual-machines.json")
                slurp
                (json/read-str :key-fn keyword))
        timestamp (time/now)
        xf (t/insert-action-xf timestamp)]

    ;; check transform with a sequence
    (let [results (sequence xf [doc])]
      (is (= 10 (count results)))
      (doseq [result results]
        (is (= t/index-action (first result)))
        (is (= timestamp (:snapshot-time (second result))))))

    ;; check transform on channel
    (let [ch (async/chan 1 xf)]
      (async/>!! ch doc)
      (async/close! ch)
      (let [n (loop [n 0]
                (let [result (async/<!! ch)]
                  (if (nil? result)
                    n
                    (do
                      (is (= t/index-action (first result)))
                      (is (= timestamp (:snapshot-time (second result))))
                      (recur (inc n))))))]
        (is (= 10 n))))))

