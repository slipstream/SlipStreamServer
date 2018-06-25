(ns com.sixsq.slipstream.ssclj.resources.spec.common-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.spec.acl :as cimi-acl]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(deftest check-nonblank-string
  (doseq [v #{"ok" " ok" "ok " " ok "}]
    (stu/is-valid ::cimi-core/nonblank-string v))

  (doseq [v #{"" " " "\t" "\f" "\t\f"}]
    (stu/is-invalid ::cimi-core/nonblank-string v)))


(deftest check-timestamp
  (stu/is-valid ::cimi-core/timestamp "2012-01-01T01:23:45.678Z")
  (stu/is-invalid ::cimi-core/timestamp "2012-01-01T01:23:45.678Q"))


(deftest check-resource-link
  (doseq [v #{{:href "uri"}, {:href "uri" :ok "value"}}]
    (stu/is-valid ::cimi-common/resource-link v))

  (doseq [v #{{}, {:bad "value"}, {:href ""}}]
    (stu/is-invalid ::cimi-common/resource-link v)))


(deftest check-resource-links
  (stu/is-valid ::cimi-common/resource-links [{:href "uri"}])
  (stu/is-valid ::cimi-common/resource-links [{:href "uri"} {:href "uri"}])
  (stu/is-invalid ::cimi-common/resource-links []))


(deftest check-operation
  (stu/is-valid ::cimi-common/operation {:href "uri" :rel "add"})
  (stu/is-invalid ::cimi-common/operation {:href "uri"})
  (stu/is-invalid ::cimi-common/operation {:rel "add"})
  (stu/is-invalid ::cimi-common/operation {}))


(deftest check-operations
  (stu/is-valid ::cimi-common/operations [{:href "uri" :rel "add"}])
  (stu/is-valid ::cimi-common/operations [{:href "uri" :rel "add"} {:href "uri" :rel "delete"}])
  (stu/is-invalid ::cimi-common/operations []))


(deftest check-properties
  (doseq [v #{{:a "ok"}, {:a "ok" :b "ok"}, {"a" "ok"}, {"a" "ok" "b" "ok"}}]
    (stu/is-valid ::cimi-common/properties v))

  (doseq [v #{{}, {1 "bad"}, {"bad" 1}, [:bad "bad"]}]
    (stu/is-invalid ::cimi-common/properties v)))


(deftest check-owner
  (let [id {:principal "ADMIN", :type "ROLE"}]

    (stu/is-valid ::cimi-acl/owner id)

    (doseq [k #{:principal :type}]
      (stu/is-invalid ::cimi-acl/owner (dissoc id k)))

    (doseq [v #{{:bad "MODIFY"}, {:type "BAD"}}]
      (stu/is-invalid ::cimi-acl/owner (merge id v)))))


(deftest check-rule
  (let [rule {:principal "ADMIN", :type "ROLE", :right "VIEW"}]

    (stu/is-valid ::cimi-acl/rule rule)

    (doseq [v #{"MODIFY" "ALL"}]
      (stu/is-valid ::cimi-acl/rule (merge rule {:right v})))

    (doseq [v #{"BAD" nil}]
      (stu/is-invalid ::cimi-acl/rule (merge rule {:right v})))))


(deftest check-rules
  (let [rules [{:principal "ADMIN", :type "ROLE", :right "VIEW"}
               {:principal "ALPHA", :type "USER", :right "ALL"}]]

    (stu/is-valid ::cimi-acl/rules rules)
    (stu/is-valid ::cimi-acl/rules (next rules))

    (stu/is-invalid ::cimi-acl/rules (nnext rules))
    (stu/is-invalid ::cimi-acl/rules (cons 1 rules))))


(deftest check-acl
  (let [acl {:owner {:principal "ADMIN"
                     :type      "ROLE"}
             :rules [{:principal "group1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "group2"
                      :type      "ROLE"
                      :right     "MODIFY"}]}]

    (stu/is-valid ::cimi-common/acl acl)
    (stu/is-valid ::cimi-common/acl (dissoc acl :rules))

    (doseq [v #{{:rules []}, {:owner ""}, {:bad "BAD"}}]
      (stu/is-invalid ::cimi-common/acl (merge acl v)))))


(s/def ::common-attrs (su/only-keys-maps cimi-common/common-attrs))

(deftest check-common-attrs
  (let [date "2012-01-01T01:23:45.678Z"
        acl {:owner {:principal "ADMIN"
                     :type      "ROLE"}
             :rules [{:principal "group1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "group2"
                      :type      "ROLE"
                      :right     "MODIFY"}]}
        minimal {:id          "a"
                 :resourceURI "http://example.org/data"
                 :created     date
                 :updated     date
                 :acl         acl}
        maximal (assoc minimal
                  :name "name"
                  :description "description"
                  :properties {"a" "b"}
                  :operations [{:rel "add" :href "/add"}]
                  :acl acl)]

    (stu/is-valid ::common-attrs minimal)

    (stu/is-valid ::common-attrs maximal)

    (stu/is-invalid ::common-attrs (assoc maximal :bad "BAD"))

    (doseq [k #{:id :resourceURI :created :updated}]
      (stu/is-invalid ::common-attrs (dissoc minimal k)))

    (doseq [k #{:name :description :properties}]
      (stu/is-valid ::common-attrs (dissoc maximal k)))))
