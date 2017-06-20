(ns com.sixsq.slipstream.ssclj.usage.utils-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :refer :all]
    [clojure.test :refer :all]))

(deftest test-into-without-nil
  (is (nil? (into-vec-without-nil :a nil)))
  (is (nil? (into-vec-without-nil :a [])))
  (is (nil? (into-vec-without-nil :a '())))
  (is (nil? (into-vec-without-nil :a [nil])))

  (is (= [:a 1 2 3] (into-vec-without-nil :a [1 2 3])))
  (is (= [:a 1 2 3] (into-vec-without-nil :a [1 2 3 nil])))
  (is (= [:a 1 2 3] (into-vec-without-nil :a [nil 1 nil 2 3 nil])))

  (is (vector? (into-vec-without-nil :a [1 2 3 nil]))))
