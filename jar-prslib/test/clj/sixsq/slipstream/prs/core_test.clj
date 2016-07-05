(ns sixsq.slipstream.prs.core-test
  (:require [clojure.test :refer :all]
            [sixsq.slipstream.prs.core :refer :all]
            [clojure.data.json :as json]))

(deftest test-place-and-rank
  (is (= (json/write-str {}) (place-and-rank {}))))

(deftest test-prs-place-and-rank
  (is (= {:components []} (prs-place-and-rank "" {:components []})))
  (is (= {:components []} (prs-place-and-rank "" {:user-connectors []}))))


(deftest test-build-prs-input
  (is (= {:components      [{:node "node1" :comp-uri "http://a", :multiplicity 1, :policy "string"}]
          :user-connectors []}
         (build-prs-input {:prs-endpoint "http://prs-server"
                           :user-connectors []
                           :module          {:components [{:node         "node1"
                                                           :comp-uri     "http://a"
                                                           :multiplicity 1
                                                           :policy       "string"}]}}))))
