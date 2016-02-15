(ns com.sixsq.slipstream.ssclj.database.ddl-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.database.ddl :refer :all]))

(deftest double-quote-works-with-dotted-words
  (is (= "\"abc\"" (double-quote "abc")))
  (is (= "\"abc\".\"def\"" (double-quote "abc.def"))))
