(ns com.sixsq.slipstream.ssclj.es.es-filter-test
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.es.es-filter :refer :all]))

(deftest test-compile-filter
  ;; (is (compile-filter "a=1"))
  (is (= 12 (compile-filter "(a=1)")))
  ;; (is (= 1 (compile-filter "(a=1)")))
  )

;#object[org.elasticsearch.index.query.ConstantScoreQueryBuilder
;        0xa42c700
;        "{
;  \"constant_score\" : {
;    \"filter\" : {
;      \"term\" : {
;        \"a\" : 1
;      }
;    }
;  }
;}"]