(ns sixsq.slipstream.prs.core_test
  (:require [clojure.test :refer :all]
            [sixsq.slipstream.prs.core :refer :all]))

(deftest test-place-and-rank
  (is (= {} (place-and-rank {})))
  )
