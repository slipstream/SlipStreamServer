(ns com.sixsq.slipstream.ssclj.filter.performance-parser-test
  (:require
    [clojure.test :refer :all]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]))

(defn- cimi-filter
  [nb-expressions]
  (str/join " or " (repeat nb-expressions "(a='1')")))

(deftest test-parse-performance
  ; Shows that performance of parsing decreases very quickly
  ; some numbers (vary greatly, just an idea)
  ; looks like it's o(2^n)
  ; nb  : time (ms)
  ; 10  : 6
  ; 11  : 8
  ; 12  : 15
  ; 13  : 30
  ; 14  : 55
  ; 15  : 100
  ; 16  : 250
  ; 17  : 500
  ; 20  : 6000
  ; 30  : java.lang.OutOfMemoryError: GC overhead limit exceeded
  (time (parser/parse-cimi-filter (cimi-filter 2))))
