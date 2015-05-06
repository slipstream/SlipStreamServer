(ns com.sixsq.slipstream.ssclj.usage.utils-test
  (:require 
    [com.sixsq.slipstream.ssclj.usage.utils :refer :all]    
    [clojure.test :refer :all]))

  (deftest walk-clojurify-should-recursively-keywordize-a-java-map
    (is (=
      {:A 42 :c {:B [{:Z 111} {:e 1515}]}}
      (walk-clojurify 
        (java.util.HashMap. {"A" 42 "c" {"B" (java.util.ArrayList. [(java.util.HashMap. {"Z" 111}) {"e" 1515}])}})))))
