(ns sixsq.slipstream.prs.core_test
  (:require [clojure.test :refer :all]
            [sixsq.slipstream.prs.core :refer :all]
            [clojure.data.json :as json])
  )

(deftest test-place-and-rank
  (is (= (json/write-str {}) (place-and-rank {})))
  )
