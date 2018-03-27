(ns com.sixsq.slipstream.db.serializers.service-config-impl-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]))

(defn not-empty-string?
  [x]
  (and (string? x) (not (empty? x))))

(deftest test-grow-coll-1->2
  (is (= [] (sci/grow-coll-1->2 [])))
  (is (= [:a :b] (sci/grow-coll-1->2 [:a :b])))
  (is (= [:a :b :c] (sci/grow-coll-1->2 [:a :b :c])))
  (is (= [:d :d] (sci/grow-coll-1->2 [:d]))))

(deftest test-connector-names-map
    (is (= {} (sci/connector-names-map "")))
    (is (= {"foo" "foo"} (sci/connector-names-map "foo")))
    (is (= {"foo" "bar"} (sci/connector-names-map "foo:bar")))
    (is (= {"foo" "bar"} (sci/connector-names-map "foo:bar,")))
    (is (= {"foo" "bar" "baz" "baz"} (sci/connector-names-map "foo:bar,baz")))
    (is (= {"foo" "bar" "baz" "baz"} (sci/connector-names-map "foo:bar, ,baz")))
    (is (= {"foo" "bar" "baz" "baz"} (sci/connector-names-map " foo:bar,\n baz\n\t"))))

(deftest test-pname-to-kwname
  (is (= "" (sci/conn-pname-to-kwname "" "")))
  (is (= "" (sci/conn-pname-to-kwname "foo" "")))
  (is (= "bar" (sci/conn-pname-to-kwname "foo" "bar")))
  (is (= "fooBar" (sci/conn-pname-to-kwname "" "foo.bar")))
  (is (= "fooBar" (sci/conn-pname-to-kwname "" "foo-bar")))
  (is (= "fooBar" (sci/conn-pname-to-kwname "" "foo.-bar")))
  (is (= "fooBar" (sci/conn-pname-to-kwname "" "foo-.bar")))
  (is (= "fooBarBaz" (sci/conn-pname-to-kwname "" "foo.bar.baz")))
  (is (= "fooBarBazQuix" (sci/conn-pname-to-kwname "" "foo.bar-baz.quix")))
  (is (= "fooBarBazQuix" (sci/conn-pname-to-kwname "" "foo-bar-baz-quix"))))

(deftest test-strip-unwanted-attrs
  (is (= {} (sci/strip-unwanted-attrs {})))
  (is (= {:foo "bar"} (sci/strip-unwanted-attrs {:foo "bar"})))
  (is (= {:foo "bar"} (sci/strip-unwanted-attrs {:foo "bar" :id 123})))
  (is (= {:id 123} (sci/strip-unwanted-attrs {:foo "bar" :id 123} #{:foo})))
  (is (= {} (sci/strip-unwanted-attrs {:foo "bar" :id 123} #{:foo :id}))))
