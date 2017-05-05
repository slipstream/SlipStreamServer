(ns sixsq.slipstream.placement.core-test
  (:require
    [clojure.test :refer :all]
    [sixsq.slipstream.placement.core :as pc]))

(deftest check-number-or-nil
  (let [nils [nil -1 -1.0 -3/4 "a" true ["some" "values"]]
        selves [nil 0 0.0 1 1.0 3/4]]
    (doseq [v nils]
      (is (nil? (pc/number-or-nil v))))
    (doseq [v selves]
      (is (= v (pc/number-or-nil v))))))

(deftest check-price-comparator
  (is (pc/price-comparator 0 1))
  (is (not (pc/price-comparator 1 0)))
  (is (not (pc/price-comparator 0 0)))
  (is (not (pc/price-comparator nil nil)))
  (is (pc/price-comparator 0 nil))
  (is (not (pc/price-comparator nil 0)))
  (is (pc/price-comparator 0 "a"))
  (is (not (pc/price-comparator "a" 0))))

(deftest check-order-by-price
  (let [values [{:order 1 :price 0}
                {:order 2 :price 4/2}
                {:order 3 :price 3.0}
                {:order 4 :price nil}
                {:order 4 :price -1}
                {:order 4 :price -1.0}
                {:order 4 :price "1.0"}]]
    (doseq [coll (repeatedly 20 (partial shuffle values))]
      (is (apply <= (map :order (pc/order-by-price coll)))))))

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
  (let [so1 {:resource:vcpu 1
              :resource:ram  4
              :resource:disk 10}
        so2 {:resource:vcpu 8
              :resource:ram  32
              :resource:disk 500}
        so3 {:resource:vcpu 2
              :resource:ram  8
              :resource:disk 20}]
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
  (is (= [{:exoscale:instanceType "small"}]
         (pc/prefer-exact-instance-type {:exo "small" :ec2 "insanely-huge"}
                                        ["exo" [{:exoscale:instanceType "small"} {:exoscale:instanceType "big"}]])))
  ; extra is absent, list returned unchanged
  (is (= [{:exoscale:instanceType "small"} {:exoscale:instanceType "big"}]
         (pc/prefer-exact-instance-type {:exo "extra" :ec2 "insanely-huge"}
                                        ["exo" [{:exoscale:instanceType "small"} {:exoscale:instanceType "big"}]]))))
