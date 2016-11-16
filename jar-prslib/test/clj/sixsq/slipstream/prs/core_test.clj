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
  (is (= {:components      [{:module           "module/component2"
                             :placement-policy "schema-org:location='de'"
                             :cpu.nb           "1"
                             :ram.GB           "4"
                             :disk.GB          "10"}]
          :user-connectors ["exo1" "ec1-eu-west"]}
         (build-prs-input
           {:prs-endpoint    "http://prs-server"
            :user-connectors ["exo1" "ec1-eu-west"]
            :components      [{:module           "module/component2"
                               :placement-policy "schema-org:location='de'"
                               :cpu.nb           "1"
                               :ram.GB           "4"
                               :disk.GB          "10"}]}))))
