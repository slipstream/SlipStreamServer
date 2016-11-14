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
  (let [connectors [{:user-connector "exoscale", :vm-sizes {(keyword "module/p1/image1/48") "micro"}}
                    {:user-connector "test-cheap", :vm-sizes {(keyword "module/p1/image1/48") "Xmicro"}}]
        component {:module "module/p1/image1/48", :vm-size "unused", :placement-policy "location='ch'"}
        component-no-placement {:module "module/p1/image1/48", :vm-size "unused", :placement-policy ""}]
    (is (= "(location='ch') and (connector/href='exoscale' or connector/href='test-cheap')"
           (pc/cimi-filter-policy connectors component)))
    (is (= "connector/href='exoscale' or connector/href='test-cheap'"
           (pc/cimi-filter-policy connectors component-no-placement)))))

(deftest test-filter-user-connectors
  (let [user-connectors [{:user-connector "exoscale", :vm-sizes {(keyword "module/p1/image1/48") "micro"}}
                         {:user-connector "test-cheap", :vm-sizes {(keyword "module/p1/image1/48") "Xmicro"}}]]
    (is (= user-connectors (pc/filter-user-connectors user-connectors {} {:placement-policy ""})))))

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
