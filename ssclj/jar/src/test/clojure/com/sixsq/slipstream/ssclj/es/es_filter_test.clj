(ns com.sixsq.slipstream.ssclj.es.es-filter-test
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.es.es-filter :as esf]))

(deftest test-attribute-conversion
  (is (= [:Attribute "att"] (esf/convert [:Attribute "att"])))
  (is (= [:Attribute "ns1:att1"] (esf/convert [:Attribute "ns1:att1"])))
  (is (= [:Attribute "ns1:att1.ns2:att2"] (esf/convert [:Attribute "ns1:att1/ns2:att2"]))))


