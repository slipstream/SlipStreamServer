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
  (is (= "" (pc/cimi-and [nil nil])))
  (is (= "" (pc/cimi-and ["" nil])))
  (is (= "" (pc/cimi-and [nil ""])))

  (is (= "(location='de')" (pc/cimi-and [nil "location='de'"])))
  (is (= "(location='de')" (pc/cimi-and ["location='de'" nil])))
  (is (= "(location='de')" (pc/cimi-and ["location='de'" ""])))
  (is (= "(location='de')" (pc/cimi-and ["" "location='de'"])))

  (is (= "(location='de') and (price=1)" (pc/cimi-and ["location='de'" "price=1"])))
  (is (= "(location='de') and (price=1) and (a=2)" (pc/cimi-and ["location='de'" "price=1" "a=2"])))
  (is (= "(location='de') and (price=1) and (a=2)" (pc/cimi-and [nil "location='de'" nil "price=1" "a=2"]))))

(deftest test-cimi-or
  (is (= "" (pc/cimi-or [nil nil])))
  (is (= "" (pc/cimi-or ["" nil])))
  (is (= "" (pc/cimi-or [nil ""])))

  (is (= "(location='de')" (pc/cimi-or [nil "location='de'"])))
  (is (= "(location='de')" (pc/cimi-or ["location='de'" nil])))
  (is (= "(location='de')" (pc/cimi-or ["location='de'" ""])))
  (is (= "(location='de')" (pc/cimi-or ["" "location='de'"])))

  (is (= "(location='de') or (price=1)" (pc/cimi-or ["location='de'" "price=1"]))))

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
    (is (nil? (pc/smallest-service-offer [])))
    (is (= so1 (pc/smallest-service-offer [so1 so2 so3])))
    (is (= so1 (pc/smallest-service-offer [so2 so1 so3])))
    (is (= so1 (pc/smallest-service-offer [so3 so2 so1])))
    (is (= so1 (pc/smallest-service-offer [so3 so1 so2])))
    (is (= so3 (pc/smallest-service-offer [so2 so3])))))

(deftest test-denamespace-keys
  (is (= {} (pc/denamespace-keys {})))
  (is (= {:a 1} (pc/denamespace-keys {:a 1})))
  (is (= {:a 1} (pc/denamespace-keys {:namespace:a 1})))
  (is (= {:a {:b 1}} (pc/denamespace-keys {:namespace:a {:namespace:b 1}}))))

(deftest test-prefer-exact-instance-type
  ; small is preffered for exo, it is kept
  (is (= [{:schema-org:name "small"}]
         (pc/prefer-exact-instance-type {:exo "small" :ec2 "insanely-huge"}
                                        ["exo" [{:schema-org:name "small"} {:schema-org:name "big"}]])))
  ; extra is absent, list returned unchanged
  (is (= [{:schema-org:name "small"} {:schema-org:name "big"}]
         (pc/prefer-exact-instance-type {:exo "extra" :ec2 "insanely-huge"}
                                        ["exo" [{:schema-org:name "small"} {:schema-org:name "big"}]]))))
