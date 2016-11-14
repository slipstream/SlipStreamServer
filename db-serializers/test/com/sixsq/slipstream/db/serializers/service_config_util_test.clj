(ns com.sixsq.slipstream.db.serializers.service-config-util-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu]))

(def xml-entry
  {:tag   :entry,
   :attrs {},
   :content
          [{:tag     :string,
            :attrs   {},
            :content ["ec2-us-west-2.security.groups"]},
           {:tag     :parameter,
            :attrs   {:foo "bar"},
            :content [{:tag :value, :attrs {}, :content ["slipstream_managed"]}
                      {:tag :foo, :attrs {}, :content ["bar"]}
                      {:tag :instructions, :attrs {}, :content ["do this"]}
                      {:tag   :enumValues,
                       :attrs {:length "2"},
                       :content
                              [{:tag :string, :attrs {}, :content ["foo"]}
                               {:tag :string, :attrs {}, :content ["bar"]}]}]}]})

(def xml-entries
  {:content [{:content [xml-entry]}]})

(deftest test-xml-param-attrs
  (is (nil? (scu/xml-param-attrs nil)))
  (is (= {:foo "bar"} (scu/xml-param-attrs xml-entry))))

(deftest test-xml-param-value
  (is (nil? (scu/xml-param-value nil)))
  (is (= "slipstream_managed" (scu/xml-param-value xml-entry))))

(deftest test-xml-param-instructions
  (is (nil? (scu/xml-param-instructions nil)))
  (is (= "do this" (scu/xml-param-instructions xml-entry))))

(deftest test-xml-param-enum-values
  (is (= [] (scu/xml-param-enum-values nil)))
  (is (= ["foo" "bar"] (scu/xml-param-enum-values xml-entry))))

(deftest test-xml-params-parse
  (is (= [[{:foo "bar"} "slipstream_managed" "do this" ["foo" "bar"]]] (scu/xml-params-parse xml-entries))))
