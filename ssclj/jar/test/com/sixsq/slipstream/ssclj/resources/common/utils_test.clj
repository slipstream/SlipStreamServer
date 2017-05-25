(ns com.sixsq.slipstream.ssclj.resources.common.utils-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as r]))

(deftest check-de-camelcase
  (are [expect arg] (= expect (r/de-camelcase arg))
                    "" ""
                    "abc" "Abc"
                    "abc-def" "AbcDef"))

;; given string must be lisp-cased, if not empty string returned
(deftest check-lisp-to-camelcase
  (are [expect arg] (= expect (r/lisp-to-camelcase arg))
                    "" "Abc"
                    "" "-"
                    "" "abc-"
                    "" "abc--def"
                    "" "abc-Def-ghi"
                    "" ""
                    "Abc" "abc"
                    "AbcDef" "abc-def"))
