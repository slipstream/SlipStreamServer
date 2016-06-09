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

