(ns com.sixsq.slipstream.ssclj.resources.common.schema-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.common.schema :refer :all]
    [schema.core :as s]
    [clojure.set :as set]))

(def non-nil? (complement nil?))

(deftest check-actions
  (is (= (set/union core-actions prefixed-actions impl-prefixed-actions) (set (keys action-uri))))
  (is (= (set (map name core-actions)) (set (vals (select-keys action-uri core-actions))))))

(deftest check-PosInt
  (are [expect-fn arg] (expect-fn (s/check PosInt arg))
                       nil? 1
                       nil? 2
                       nil? 3
                       non-nil? 0
                       non-nil? -1
                       non-nil? 1.0
                       non-nil? "bad"))

(deftest check-NonNegInt
  (are [expect-fn arg] (expect-fn (s/check NonNegInt arg))
                       nil? 0
                       nil? 1
                       nil? 2
                       nil? 3
                       non-nil? -1
                       non-nil? 1.0
                       non-nil? "bad"))

(deftest check-NonBlankString
  (are [expect-fn arg] (expect-fn (s/check NonBlankString arg))
                       nil? "ok"
                       nil? " ok"
                       nil? "ok "
                       nil? " ok "
                       non-nil? ""
                       non-nil? " "
                       non-nil? "\t"
                       non-nil? "\f"
                       non-nil? "\t\f"))

(deftest check-NonEmptyStrList
  (are [expect-fn arg] (expect-fn (s/check NonEmptyStrList arg))
                       nil? ["ok"]
                       nil? ["ok" "ok"]
                       non-nil? []
                       non-nil? [1]
                       non-nil? ["ok" 1]))

(deftest check-Timestamp
  (are [expect-fn arg] (expect-fn (s/check Timestamp arg))
                       nil? "2012-01-01T01:23:45.678Z"
                       non-nil? "2012-01-01T01:23:45.678Q"))

(deftest check-ResourceLink
  (are [expect-fn arg] (expect-fn (s/check ResourceLink arg))
                       nil? {:href "uri"}
                       non-nil? {}
                       non-nil? {:bad "value"}
                       non-nil? {:href ""}
                       non-nil? {:href "uri" :bad "value"}))

(deftest check-ResourceLinks
  (are [expect-fn arg] (expect-fn (s/check ResourceLinks arg))
                       nil? [{:href "uri"}]
                       nil? [{:href "uri"} {:href "uri"}]
                       non-nil? []))

(deftest check-Operation
  (are [expect-fn arg] (expect-fn (s/check Operation arg))
                       nil? {:href "uri" :rel "add"}
                       non-nil? {:href "uri"}
                       non-nil? {:rel "add"}
                       non-nil? {}))

(deftest check-Operations
  (are [expect-fn arg] (expect-fn (s/check Operations arg))
                       nil? [{:href "uri" :rel "add"}]
                       nil? [{:href "uri" :rel "add"} {:href "uri" :rel "delete"}]
                       non-nil? []))

(deftest check-Properties
  (are [expect-fn arg] (expect-fn (s/check Properties arg))
                       nil? {:a "ok"}
                       nil? {:a "ok" :b "ok"}
                       nil? {"a" "ok"}
                       nil? {"a" "ok" "b" "ok"}
                       non-nil? {}
                       non-nil? {1 "ok"}
                       non-nil? {"ok" 1}
                       non-nil? [:bad "bad"]))

(def valid-acl {:owner {:principal "me" :type "USER"}})

(deftest check-AccessControlId
  (let [id {:principal "ADMIN", :type "ROLE"}]
    (are [expect-fn arg] (expect-fn (s/check AccessControlId arg))
                         nil? id
                         non-nil? (assoc id :bad "MODIFY")
                         non-nil? (dissoc id :principal)
                         non-nil? (dissoc id :type)
                         non-nil? (assoc id :type "BAD"))))

(deftest check-AccessControlRule
  (let [rule {:principal "ADMIN", :type "ROLE", :right "VIEW"}]
    (are [expect-fn arg] (expect-fn (s/check AccessControlRule arg))
                         nil? rule
                         nil? (assoc rule :right "MODIFY")
                         nil? (assoc rule :right "ALL")
                         non-nil? (assoc rule :right "BAD")
                         non-nil? (dissoc rule :right))))

(deftest check-AccessControlRules
  (let [rules [{:principal "ADMIN", :type "ROLE", :right "VIEW"}
               {:principal "ALPHA", :type "USER", :right "ALL"}]]
    (are [expect-fn arg] (expect-fn (s/check AccessControlRules arg))
                         nil? rules
                         nil? (next rules)
                         non-nil? (nnext rules)
                         non-nil? (cons 1 rules))))

(deftest check-AccessControlList
  (let [acl {:owner {:principal "::ADMIN"
                     :type      "ROLE"}
             :rules [{:principal ":group1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "group2"
                      :type      "ROLE"
                      :right     "MODIFY"}]}]
    (are [expect-fn arg] (expect-fn (s/check AccessControlList arg))
                         nil? acl
                         nil? (dissoc acl :rules)
                         non-nil? (assoc acl :rules [])
                         non-nil? (assoc acl :owner "")
                         non-nil? (assoc acl :bad "BAD"))))

(deftest check-acl-attr
  (is (nil? (s/check AclAttr {:acl valid-acl})))
  (is (s/check AclAttr {})))

(deftest check-CommonAttrs
  (let [date "2012-01-01T01:23:45.678Z"
        minimal {:id          "a"
                 :resourceURI "http://example.org/data"
                 :created     date
                 :updated     date}
        maximal (assoc minimal
                  :name "name"
                  :description "description"
                  :properties {"a" "b"}
                  :operations [{:rel "add" :href "/add"}])]
    (are [expect-fn arg] (expect-fn (s/check CommonAttrs arg))
                         nil? minimal
                         non-nil? (dissoc minimal :id)
                         non-nil? (dissoc minimal :resourceURI)
                         non-nil? (dissoc minimal :created)
                         non-nil? (dissoc minimal :updated)
                         nil? maximal
                         nil? (dissoc maximal :name)
                         nil? (dissoc maximal :description)
                         nil? (dissoc maximal :properties)
                         non-nil? (assoc maximal :bad "BAD"))))

(deftest check-ParameterTypes
  (are [expect-fn arg] (expect-fn (s/check ParameterTypes arg))
                       nil? "string"
                       nil? "boolean"
                       nil? "int"
                       nil? "float"
                       nil? "timestamp"
                       nil? "enum"
                       nil? "map"
                       nil? "list"
                       non-nil? "unknown"))

(deftest check-ParameterDescription-and-ResourceDescription
  (let [valid-description {:displayName "ID"
                           :category    "common"
                           :description "unique resource identifier"
                           :type        "enum"
                           :mandatory   true
                           :readOnly    true
                           :order       0
                           :enum        ["a" "b" "c"]}
        resource-desc {:identifier valid-description
                       :other      valid-description
                       :acl        valid-acl}]

    (are [expect-fn arg] (expect-fn (s/check ParameterDescription arg))
                         nil? valid-description
                         nil? (dissoc valid-description :category)
                         nil? (dissoc valid-description :description)
                         nil? (dissoc valid-description :mandatory)
                         nil? (dissoc valid-description :readOnly)
                         nil? (dissoc valid-description :order)
                         nil? (dissoc valid-description :enum)
                         non-nil? (assoc valid-description :displayName 1)
                         non-nil? (assoc valid-description :category 1)
                         non-nil? (assoc valid-description :description 1)
                         non-nil? (assoc valid-description :type "unknown")
                         non-nil? (assoc valid-description :mandatory 1)
                         non-nil? (assoc valid-description :readOnly 1)
                         non-nil? (assoc valid-description :readOnly "1")
                         non-nil? (assoc valid-description :enum "1")
                         non-nil? (assoc valid-description :enum ["a" 1]))

    (are [expect-fn arg] (expect-fn (s/check ResourceDescription arg))
                         nil? resource-desc
                         non-nil? (assoc resource-desc :another 1)
                         nil? (assoc CommonParameterDescription :acl valid-acl))))
