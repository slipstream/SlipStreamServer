(ns sixsq.slipstream.pricing.lib.pricing-test
  (:require [clojure.test :refer :all]
            [sixsq.slipstream.pricing.lib.pricing :refer :all]))

(def ^:private ^:const float-precision 1e-8)

(defn- floating=
  [a b]
  (-> a (- b) Math/abs (< float-precision)))

(deftest test-compute-cost
  (let [cloud-cost {:billingPeriodCode  "MIN"
                    :billingUnitCode    "MIN"
                    :discount           {:method "None" :reset false :steps []}
                    :freeUnits          0
                    :unitCost           0.12
                    :unitCode           "C62"}
        cloud-cost-without-discount {:billingPeriodCode  "MIN"
                                     :billingUnitCode    "MIN"
                                     :freeUnits          0
                                     :unitCost           0.12
                                     :unitCode           "C62"}]
    (is (floating= 0.12 (compute-cost cloud-cost [{:timeCode "MIN"
                                                   :sample   1
                                                   :values   [1]}])))
    (is (floating= 0.12 (compute-cost cloud-cost-without-discount [{:timeCode "MIN"
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


