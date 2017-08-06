(ns com.sixsq.slipstream.ssclj.resources.common.utils-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as t]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clj-time.core :as time]))

(deftest check-expired?-and-not-expired?
  (let [past-time (-> 10 time/minutes time/ago u/unparse-timestamp-datetime)
        future-time (-> 10 time/minutes time/from-now u/unparse-timestamp-datetime)]
    (is (false? (t/expired? nil)))
    (is (true? (t/expired? past-time)))
    (is (false? (t/expired? future-time)))
    (is (true? (t/not-expired? nil)))
    (is (false? (t/not-expired? past-time)))
    (is (true? (t/not-expired? future-time)))))

(deftest check-de-camelcase
  (are [expect arg] (= expect (t/de-camelcase arg))
                    "" ""
                    "abc" "Abc"
                    "abc-def" "AbcDef"))

;; given string must be lisp-cased, if not empty string returned
(deftest check-lisp-to-camelcase
  (are [expect arg] (= expect (t/lisp-to-camelcase arg))
                    "" "Abc"
                    "" "-"
                    "" "abc-"
                    "" "abc--def"
                    "" "abc-Def-ghi"
                    "" ""
                    "Abc" "abc"
                    "AbcDef" "abc-def"))
