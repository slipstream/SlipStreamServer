(ns com.sixsq.slipstream.ssclj.resources.common.utils-expectations-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(deftest check-encoding
  (let [round-trip (comp decode-base64 encode-base64)
        values ["alpha"
                2
                true
                3.14
                {:alpha "alpha"}]]

    (doseq [v values]
           (is (= v (round-trip v))))))

(deftest check-de-camelcase
  (are [expect arg] (= expect (u/de-camelcase arg))
                    "" ""
                    "abc" "Abc"
                    "abc-def" "AbcDef"))

;; given string must be lisp-cased, if not empty string returned
(deftest check-list-to-camelcase
  (are [expect arg] (= expect (u/lisp-to-camelcase arg))
                    "" "Abc"
                    "" "-"
                    "" "abc-"
                    "" "abc--def"
                    "" "abc-Def-ghi"
                    "" ""
                    "Abc" "abc"
                    "AbcDef" "abc-def"))

(deftest check-serialize
  (is (= "{\"http:\\/\\/example.org\\/a\\/b.json\":\"truc\"}\n"
         (u/serialize {:http://example.org/a/b.json "truc"})))
  (is (= "{\"http:\\/\\/example.org\\/a\\/b.json\":\"truc\"}\n"
         (u/serialize {"http://example.org/a/b.json" "truc"}))))
