(ns com.sixsq.slipstream.tools.cli.ssconfig-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.tools.cli.ssconfig :as c]))

(deftest test-resource-type
  (is (= "" (c/resource-type {})))
  (is (= "foo" (c/resource-type {:id "foo/bar"})))
  (is (= "foo" (c/resource-type {:body {:id "foo/bar"}}))))
