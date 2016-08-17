(ns com.sixsq.slipstream.ssclj.resources.common.utils-expectations-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :refer :all]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; Base64 encoding
;;

(let [round-trip (comp decode-base64 encode-base64)
      values     ["alpha"
                  2
                  true
                  3.14
                  {:alpha "alpha"}]]

  (expect (from-each [v values]
                     (= v (round-trip v)))))

(expect "" (u/de-camelcase ""))
(expect "abc" (u/de-camelcase "Abc"))
(expect "abc-def" (u/de-camelcase "AbcDef"))

;; given string must be lisp-cased, if not empty string returned
(expect "" (u/lisp-to-camelcase "Abc"))
(expect "" (u/lisp-to-camelcase "-"))
(expect "" (u/lisp-to-camelcase "abc-"))
(expect "" (u/lisp-to-camelcase "abc--def"))
(expect "" (u/lisp-to-camelcase "abc-Def-ghi"))
(expect "" (u/lisp-to-camelcase ""))

(expect "Abc" (u/lisp-to-camelcase "abc"))
(expect "AbcDef" (u/lisp-to-camelcase "abc-def"))

(run-all-tests)

