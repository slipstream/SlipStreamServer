(ns com.sixsq.slipstream.db.es.pagination-test
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.es.pagination :as t]))

(defn- first-last->from-size
  [[first last]]
  (t/from-size {:cimi-params {:first first :last last}}))

(defn- is-first-last->from-size
  [[first last] [from size]]
  (is (= [from size] (first-last->from-size [first last]))))

(deftest from-size-nominal-cases
  (is-first-last->from-size [1 1] [0 1])
  (is-first-last->from-size [1 10] [0 10]))

(deftest from-size-inconsistent-values
  (is-first-last->from-size [0 1] [0 0])
  (is-first-last->from-size [1 0] [0 0])
  (is-first-last->from-size [10 0] [0 0])
  (is-first-last->from-size [0 -1] [0 0])
  (is-first-last->from-size [-1 1] [0 0]))

(deftest from-size-incomplete-params
  (is-first-last->from-size [nil nil] [0 t/max-return-size])
  (is-first-last->from-size [nil 10] [0 10])
  (is-first-last->from-size [3 nil] [2 t/max-return-size]))

(deftest from-size-last-too-big
  (is-first-last->from-size [1 t/max-return-size] [0 t/max-return-size])
  (is (thrown? IllegalArgumentException
               (first-last->from-size [1 (inc t/max-return-size)]))))
