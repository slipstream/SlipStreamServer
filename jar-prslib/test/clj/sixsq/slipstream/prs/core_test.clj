(ns sixsq.slipstream.prs.core-test
  (:require [clojure.test :refer :all]
            [sixsq.slipstream.prs.core :refer :all]
            [clojure.data.json :as json])
  )

(deftest test-place-and-rank
  (is (= (json/write-str {}) (place-and-rank {})))
  )

(deftest test-prs-place-and-rank
  (is (= {:components []} (prs-place-and-rank "" {:components []})))
  (is (= {:components []} (prs-place-and-rank "" {:user-connectors []})))
  (let [prs-req (prs-place-and-rank "" {:components      [{:module "foo"} {:module "bar"}]
                                        :user-connectors ["c1" "c2"]})]
    (is (contains? prs-req :components))
    (let [components (:components prs-req)]
      (is (= 2 (count components))))
    ))

(def components {:components      [{:module "module/comp1"}
                                   {:module "module/comp2"}]
                 :user-connectors ["c1" "c2"]})

(deftest test-call-prs
  (is (contains? (call-prs "" components) :components))
  (is (= 2 (count (:components (call-prs "" components)))))
  (is (contains? (first (:components (call-prs "" components))) :module))
  (is (= "module/comp1" (:module (first (:components (call-prs "" components))))))
  (is (= "c1" (:name (first (:connectors (first (:components (call-prs "" components))))))))
  )
