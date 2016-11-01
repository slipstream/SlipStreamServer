(ns sixsq.slipstream.pricing.lib.pricing-test
  (:require [clojure.test :refer :all]
            [sixsq.slipstream.pricing.lib.pricing :refer :all]))

(def ^:private ^:const float-precision 1e-8)

(defn- floating=
  [a b]
  (-> a (- b) Math/abs (< float-precision)))

(deftest test-compute-cost
  (let [cloud-cost {:cloudname       "exoscale-ch-gva"
                    :resourcetype    "vm"
                    :resourcename    "Micro"
                    ;; used to compute
                    :sampleTimeCode  "MIN"
                    :billingTimeCode "MIN"
                    :discount        {:method "None" :reset false :steps []}
                    :freeQuantity    0
                    :price           0.12
                    :unitCode        "C62"}]
    (is (floating= 0.12 (compute-cost cloud-cost [{:timeCode "MIN"
                                                   :sample   1
                                                   :values   [1]}])))
    (is (floating= (* 2 0.12) (compute-cost cloud-cost [{:timeCode "MIN"
                                                         :sample   2
                                                         :values   [1]}])))
    (is (floating= (* 2 0.12) (compute-cost cloud-cost [{:timeCode "MIN"
                                                         :sample   1
                                                         :values   [2]}])))
    (is (floating= (* 60 2 0.12) (compute-cost cloud-cost [{:timeCode "HUR"
                                                            :sample   1
                                                            :values   [2]}])))))


