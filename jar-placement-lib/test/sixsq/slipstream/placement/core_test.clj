(ns sixsq.slipstream.placement.core-test
  (:require
    [clojure.test :refer :all]
    [sixsq.slipstream.placement.core :as pc]))

(deftest test-order-by-price
  (is (= [{:y 2 :price 2} {:x 1 :price 10}]
         (pc/order-by-price [{:x 1 :price 10} {:y 2 :price 2}])))
  (is (= [{:y 2 :price 2} {:z 3 :price 3} {:x 1 :price -1}]
         (pc/order-by-price [{:x 1 :price -1} {:z 3 :price 3} {:y 2 :price 2}]))))


(deftest test-cimi-and
  (is (= "" (pc/cimi-and nil nil)))
  (is (= "" (pc/cimi-and "" nil)))
  (is (= "" (pc/cimi-and nil "")))

  (is (= "location='de'" (pc/cimi-and nil "location='de'")))
  (is (= "location='de'" (pc/cimi-and "location='de'" nil)))
  (is (= "location='de'" (pc/cimi-and "location='de'" "")))
  (is (= "location='de'" (pc/cimi-and "" "location='de'")))

  (is (= "(location='de') and (price=1)" (pc/cimi-and "location='de'" "price=1"))))

(deftest test-cimi-clause-connectors-placement
  (let [connectors ["exoscale" "test-cheap" "micro"]
        component {:module "module/p1/image1/48"
                   :cpu.nb "2"
                   :ram.GB "16"
                   :disk.GB "100"
                   :placement-policy "location='ch'"}
        component-no-placement {:module "module/p1/image1/48"
                                :cpu.nb "1"
                                :ram.GB "4"
                                :disk.GB "50"
                                :placement-policy ""}]
    (is (= "((location='ch') and (connector/href='exoscale' or connector/href='test-cheap' or connector/href='micro')) and (schema-org:descriptionVector/schema-org:vcpu>=2andschema-org:descriptionVector/schema-org:ram>=16andschema-org:descriptionVector/schema-org:disk>=100)"
           (pc/cimi-filter-policy connectors component)))
    (is (= "(connector/href='exoscale' or connector/href='test-cheap' or connector/href='micro') and (schema-org:descriptionVector/schema-org:vcpu>=1andschema-org:descriptionVector/schema-org:ram>=4andschema-org:descriptionVector/schema-org:disk>=50)"
           (pc/cimi-filter-policy connectors component-no-placement)))))

(deftest test-equals-ignore-case?
  (is (pc/equals-ignore-case? "a" "a"))
  (is (pc/equals-ignore-case? "a" "A"))
  (is (pc/equals-ignore-case? "abcdef" "AbCdEf"))
  (is (pc/equals-ignore-case? nil nil))

  (is (not (pc/equals-ignore-case? "a" "b")))
  (is (not (pc/equals-ignore-case? "a" nil)))
  (is (not (pc/equals-ignore-case? nil "b")))

  (is (thrown? AssertionError (pc/equals-ignore-case? "a" 1)))
  (is (thrown? AssertionError (pc/equals-ignore-case? 1 "b"))))

(deftest test-smallest-service-offer
  (let [so1 {:schema-org:descriptionVector
             {:schema-org:vcpu 1
              :schema-org:ram  4
              :schema-org:disk 10}}
        so2 {:schema-org:descriptionVector
             {:schema-org:vcpu 8
              :schema-org:ram  32
              :schema-org:disk 500}}
        so3 {:schema-org:descriptionVector
             {:schema-org:vcpu 2
              :schema-org:ram  8
              :schema-org:disk 20}}]
    (is nil? (pc/smallest-service-offer []))
    (is (= so1 (pc/smallest-service-offer [so1 so2 so3])))
    (is (= so1 (pc/smallest-service-offer [so2 so1 so3])))
    (is (= so1 (pc/smallest-service-offer [so3 so2 so1])))
    (is (= so1 (pc/smallest-service-offer [so3 so1 so2])))
    (is (= so3 (pc/smallest-service-offer [so2 so3])))))