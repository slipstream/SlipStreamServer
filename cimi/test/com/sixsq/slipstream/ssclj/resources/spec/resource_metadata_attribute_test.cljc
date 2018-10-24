(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-attribute-test
  (:require
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-attribute :as spec]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid {:name              "my-action"
            :type              "string"
            :providerMandatory true
            :consumerMandatory true
            :mutable           true
            :consumerWritable  true

            :namespace         "https://sixsq.com/slipstream/"
            :description       "a wonderful attribute"
            :help              "just give me a value"
            :displayName       "my action"
            :category          "some string for a category"
            :order             10
            :enum              ["a" "b" "c"]})


(deftest check-attribute

  ;; attribute

  (stu/is-valid ::spec/attribute valid)

  (doseq [k #{:namespace :description :help :displayName :category :order :enum}]
    (stu/is-valid ::spec/attribute (dissoc valid k)))

  (doseq [k #{:name :type :providerMandatory :consumerMandatory :mutable :consumerWritable}]
    (stu/is-invalid ::spec/attribute (dissoc valid k)))

  (stu/is-invalid ::spec/attribute (assoc valid :badAttribute 1))
  (stu/is-invalid ::spec/attribute (assoc valid :name " bad name "))
  (stu/is-invalid ::spec/attribute (assoc valid :type "unknown-type"))
  (stu/is-invalid ::spec/attribute (assoc valid :namespace ""))
  (stu/is-invalid ::spec/attribute (assoc valid :order "bad-value"))
  (stu/is-invalid ::spec/attribute (assoc valid :enum []))

  ;; attribute vector

  (stu/is-valid ::spec/attributes [valid])
  (stu/is-valid ::spec/attributes [valid valid])
  (stu/is-valid ::spec/attributes (list valid))
  (stu/is-invalid ::spec/attributes []))
