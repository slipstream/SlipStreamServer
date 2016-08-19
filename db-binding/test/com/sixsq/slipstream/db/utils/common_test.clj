(ns com.sixsq.slipstream.db.utils.common-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.utils.common :as u]))

;;
;; Base64 encoding
;;

(deftest test-base64-decode-encode
  (let [round-trip (comp u/decode-base64 u/encode-base64)
        values ["alpha"
                2
                true
                3.14
                {:alpha "alpha"}]]
    (doseq [v values]
      (is (= v (round-trip v))))))

(deftest test-camelcase
  (is (= "" (u/de-camelcase "")))
  (is (= "abc" (u/de-camelcase "Abc")))
  (is (= "abc-def" (u/de-camelcase "AbcDef")))

  ;; given string must be lisp-cased, if not empty string returned
  (is (= "" (u/lisp-to-camelcase "Abc")))
  (is (= "" (u/lisp-to-camelcase "-")))
  (is (= "" (u/lisp-to-camelcase "abc-")))
  (is (= "" (u/lisp-to-camelcase "abc--def")))
  (is (= "" (u/lisp-to-camelcase "abc-Def-ghi")))
  (is (= "" (u/lisp-to-camelcase "")))

  (is (= "Abc" (u/lisp-to-camelcase "abc")))
  (is (= "AbcDef" (u/lisp-to-camelcase "abc-def"))))

