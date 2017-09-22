(ns com.sixsq.slipstream.utils-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.utils :as t]
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

(deftest check-non-collections
  (let [xf (comp (t/unwrap) (map identity))]
    (are [input] (= input (first (sequence xf [input])))
                 nil
                 1
                 1.2
                 1/2
                 "ok"
                 true
                 false)))

(deftest check-empty-collections
  (let [xf (comp (t/unwrap) (map identity))]
    (are [input] (= '() (sequence xf [input]))
                 '()
                 []
                 {}
                 #{})))

(deftest check-collections
  (let [xf (comp (t/unwrap) (map identity))]
    (are [expected input] (= expected (sequence xf input))
                          [1 2 3 4 5] [1 2 3 4 5]
                          [1 2 3 4 5] [1 2 3 [4 5]]
                          [1 2 3 4 5] [[1 2] 3 [4 5]]
                          [[1 2] 3 4 5] [[[1 2]] 3 4 5])))
