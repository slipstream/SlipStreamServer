(ns com.sixsq.slipstream.ssclj.resources.common.utils-test
  (import clojure.lang.ExceptionInfo)
  (:require
    [clojure.test                                      :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.utils :refer :all]))

(deftest offset-limit-should-extract-from-options
  (is (= {:offset 1 :limit 4}
         (offset-limit {:query-params {"$first" "2" "$last" "5"}}))))

(deftest offset-limit-option-empty
  (is (= {:offset 0 :limit 0}
         (offset-limit {}))))

(deftest offset-limit-option-invalid
  (is (= {:offset 0 :limit 2}
         (offset-limit {:query-params {"$first" "-2" "$last" "2"}}))))

(deftest offset-limit-first-not-number
  (is (thrown? ExceptionInfo
         (offset-limit {:query-params {"$first" "a" "$last" "2"}}))))

(deftest offset-limit-first-higher-than-last
  (is (neg? (:limit (offset-limit {:query-params {"$first" "4" "$last" "2"}})))))

(deftest offset-limit-first-equals-last
  (is (= {:offset 2 :limit 1}
         (offset-limit {:query-params {"$first" "3" "$last" "3"}}))))