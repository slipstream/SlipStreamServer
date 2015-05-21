(ns com.sixsq.slipstream.ssclj.resources.common.utils-expectations-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :refer :all]
    [expectations                                      :refer :all]))

;;
;; Base64 encoding
;;

(let [round-trip (comp decode-base64 encode-base64)
      values ["alpha"
              2
              true
              3.14
              {:alpha "alpha"}]]

  (expect (from-each [v values]
                     (= v (round-trip v)))))

