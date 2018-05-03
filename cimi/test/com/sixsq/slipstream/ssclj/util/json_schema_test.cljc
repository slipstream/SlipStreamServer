(ns com.sixsq.slipstream.ssclj.util.json-schema-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [spec-tools.spec :as spec]
            [com.sixsq.slipstream.ssclj.util.json-schema :as t]
           ))

;;Borrowed from https://github.com/metosin/spec-tools/blob/master/test/cljc/spec_tools/json_schema_test.cljc


(s/def ::integer integer?)
(s/def ::string string?)
(s/def ::set #{1 2 3})

(s/def ::a string?)
(s/def ::b string?)
(s/def ::c string?)
(s/def ::d string?)
(s/def ::e string?)

(s/def ::keys (s/keys :opt [::e]
                      :opt-un [::e]
                      :req [::a (or ::b (and ::c ::d))]
                      :req-un [::a (or ::b (and ::c ::d))]))

(s/def ::keys-no-req (s/keys :opt [::e]
                             :opt-un [::e]))

(deftest simple-spec-test
  (testing "primitive predicates"
    ;; You're intented to call jsc/to-json with a registered spec, but to avoid
    ;; boilerplate, we do inline specization here.
    (is (= (t/transform (s/spec int?)) {:type "integer" :format "int64"}))
    (is (= (t/transform (s/spec integer?)) {:type "integer"}))
    (is (= (t/transform (s/spec float?)) {:type "number"}))
    (is (= (t/transform (s/spec double?)) {:type "number"}))
    (is (= (t/transform (s/spec string?)) {:type "keyword"}))
    (is (= (t/transform (s/spec boolean?)) {:type "boolean"}))
    #?(:clj (is (= (t/transform (s/spec decimal?)) {:type "number" :format "double"})))
    (is (= (t/transform (s/spec inst?)) {:type "keyword"}))
    (is (= (t/transform (s/spec nil?)) {:type "null"}))
    (is (= (t/transform #{1 2 3}) {:enum [1 3 2]})))
  (testing "clojure.spec predicates"
    (is (= (t/transform (s/nilable ::string)) {:oneOf [{:type "keyword"} {:type "null"}]}))
    (is (= (t/transform (s/int-in 1 10)) {:allOf [{:type "integer" :format "int64"} {:minimum 1 :maximum 10}]})))
  (testing "simple specs"
    (is (= (t/transform ::integer) {:type "integer"}))
    (is (= (t/transform ::set) {:enum [1 3 2]})))

  (testing "clojure.specs"
    (is (= (t/transform (s/keys :req-un [::integer] :opt-un [::string]))
           {:type "object"
            :properties {"integer" {:type "integer"} "string" {:type "keyword"}}
            :required ["integer"]}))
    (is (= (t/transform ::keys)
           {:type "object"
            :title "com.sixsq.slipstream.ssclj.util.json-schema-test/keys"
            :properties {"com.sixsq.slipstream.ssclj.util.json-schema-test/a" {:type "keyword"}
                         "com.sixsq.slipstream.ssclj.util.json-schema-test/b" {:type "keyword"}
                         "com.sixsq.slipstream.ssclj.util.json-schema-test/c" {:type "keyword"}
                         "com.sixsq.slipstream.ssclj.util.json-schema-test/d" {:type "keyword"}
                         "com.sixsq.slipstream.ssclj.util.json-schema-test/e" {:type "keyword"}
                         "a" {:type "keyword"}
                         "b" {:type "keyword"}
                         "c" {:type "keyword"}
                         "d" {:type "keyword"}
                         "e" {:type "keyword"}}
            :required ["com.sixsq.slipstream.ssclj.util.json-schema-test/a"
                       "com.sixsq.slipstream.ssclj.util.json-schema-test/b"
                       "com.sixsq.slipstream.ssclj.util.json-schema-test/c"
                       "com.sixsq.slipstream.ssclj.util.json-schema-test/d"
                       "a"
                       "b"
                       "c"
                       "d"]}))
    (is (= (t/transform ::keys-no-req)
           {:type "object"
            :title "com.sixsq.slipstream.ssclj.util.json-schema-test/keys-no-req"
            :properties {"com.sixsq.slipstream.ssclj.util.json-schema-test/e" {:type "keyword"}
                         "e" {:type "keyword"}}}))
    (is (= (t/transform (s/or :int integer? :string string?))
           {:anyOf [{:type "integer"} {:type "keyword"}]}))
    (is (= (t/transform (s/and integer? pos?))
           {:allOf [{:type "integer"} {:minimum 0 :exclusiveMinimum true}]}))
    (is (= (t/transform (s/and spec/integer? pos?))
           {:allOf [{:type "integer"} {:minimum 0 :exclusiveMinimum true}]}))
    (is (= (t/transform (s/merge (s/keys :req [::integer])
                                 (s/keys :req [::string])))
           {:type "object"
            :properties {"com.sixsq.slipstream.ssclj.util.json-schema-test/integer" {:type "integer"}
                         "com.sixsq.slipstream.ssclj.util.json-schema-test/string" {:type "keyword"}}
            :required ["com.sixsq.slipstream.ssclj.util.json-schema-test/integer"
                       "com.sixsq.slipstream.ssclj.util.json-schema-test/string"]}))
    (is (= (t/transform (s/every integer?)) {:type "array" :items {:type "integer"}}))
    (is (= (t/transform (s/every-kv string? integer?))
           {:type "object" :additionalProperties {:type "integer"}}))
    (is (= (t/transform (s/coll-of string?)) {:type "array" :items {:type "keyword"}}))
    (is (= (t/transform (s/coll-of string? :into '())) {:type "array" :items {:type "keyword"}}))
    (is (= (t/transform (s/coll-of string? :into [])) {:type "array" :items {:type "keyword"}}))
    (is (= (t/transform (s/coll-of string? :into #{})) {:type "array" :items {:type "keyword"}, :uniqueItems true}))
    (is (= (t/transform (s/map-of string? integer?))
           {:type "object" :additionalProperties {:type "integer"}}))
    (is (= (t/transform (s/* integer?)) {:type "array" :items {:type "integer"}}))
    (is (= (t/transform (s/+ integer?)) {:type "array" :items {:type "integer"} :minItems 1}))
    (is (= (t/transform (s/? integer?)) {:type "array" :items {:type "integer"} :minItems 0}))
    (is (= (t/transform (s/alt :int integer? :string string?))
           {:anyOf [{:type "integer"} {:type "keyword"}]}))
    (is (= (t/transform (s/cat :int integer? :string string?))
           {:type "array"
            :items {:anyOf [{:type "integer"} {:type "keyword"}]}}))
    ;; & is broken (http://dev.clojure.org/jira/browse/CLJ-2152)
    (is (= (t/transform (s/tuple integer? string?))
           {:type "array" :items [{:type "integer"} {:type "keyword"}]}))
    ;; keys* is broken (http://dev.clojure.org/jira/browse/CLJ-2152)
    (is (= (t/transform (s/map-of string? clojure.core/integer?))
           {:type "object" :additionalProperties {:type "integer"}}))
    (is (= (t/transform (s/nilable string?))
           {:oneOf [{:type "keyword"} {:type "null"}]})))
  (testing "failing clojure.specs"
    (is (not= (t/transform (s/coll-of (s/tuple string? any?) :into {}))
              {:type "object", :additionalProperties {:type "keyword"}}))))

;; Test the example from README

(s/def ::age (s/and integer? #(> % 18)))

(def person-spec
  (ds/spec
    ::person
    {::id integer?
     :age ::age
     :name string?
     :likes {string? boolean?}
     (ds/req :languages) #{keyword?}
     (ds/opt :address) {:street string?
                        :zip string?}}))

(deftest readme-test
  (is (= {:type "object"
          :required ["com.sixsq.slipstream.ssclj.util.json-schema-test/id" "age" "name" "likes" "languages"]
          :properties
          {"com.sixsq.slipstream.ssclj.util.json-schema-test/id" {:type "integer"}
           "age" {:type "integer"}
           "name" {:type "keyword"}
           "likes" {:type "object" :additionalProperties {:type "boolean"}}
           "languages" {:type "array", :items {:type "keyword"}, :uniqueItems true}
           "address" {:type "object"
                      :required ["street" "zip"]
                      :properties {"street" {:type "keyword"}
                                   "zip" {:type "keyword"}}}}}
         (t/transform person-spec))))

(deftest additional-json-schema-data-test
  (is (= {:type "integer"}
         (t/transform
           (st/spec
             {:spec integer?
              :name "integer"
              :description "it's an int"
              :json-schema/default 42})))))

(deftest deeply-nested-test
  (is (= {:type "array"
          :items {:type "array"
                  :items {:type "array"
                          :items {:type "array"
                                  :items {:type "keyword"}}}}}
         (t/transform
           (ds/spec
             ::nested
             [[[[string?]]]])))))

(s/def ::user any?)
(s/def ::name string?)
(s/def ::parent (s/nilable ::user))
(s/def ::user (s/keys :req-un [::name ::parent]))

(deftest recursive-spec-test
  (is (= {:type "object",
          :properties {"name" {:type "keyword"}
                       "parent" {:oneOf [{} {:type "null"}]}},
          :required ["name" "parent"],
          :title "com.sixsq.slipstream.ssclj.util.json-schema-test/user"}
         (t/transform ::user))))
