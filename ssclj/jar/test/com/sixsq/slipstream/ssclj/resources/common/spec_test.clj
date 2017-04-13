(ns com.sixsq.slipstream.ssclj.resources.common.spec-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec :as s]
    [clojure.set :as set]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as t]))

(deftest check-merge-keys-specs
  (is (= {:req-un #{:a :b}, :opt-un #{:c :d}}
         (t/merge-keys-specs {:req-un #{:a :b} :opt-un #{:c}}
                             {:opt-un #{:d}}))))

(deftest check-nonblank-string
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.core/nonblank-string arg))
                       true? "ok"
                       true? " ok"
                       true? "ok "
                       true? " ok "
                       false? ""
                       false? " "
                       false? "\t"
                       false? "\f"
                       false? "\t\f"))

(deftest check-timestamp
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.core/timestamp arg))
                       true? "2012-01-01T01:23:45.678Z"
                       false? "2012-01-01T01:23:45.678Q"))

(deftest check-resource-link
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.common/resource-link arg))
                       true? {:href "uri"}
                       false? {}
                       false? {:bad "value"}
                       false? {:href ""}
                       false? {:href "uri" :bad "value"}))

(deftest check-resource-links
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.common/resource-links arg))
                       true? [{:href "uri"}]
                       true? [{:href "uri"} {:href "uri"}]
                       false? []))

(deftest check-operation
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.common/operation arg))
                       true? {:href "uri" :rel "add"}
                       false? {:href "uri"}
                       false? {:rel "add"}
                       false? {}))

(deftest check-operations
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.common/operations arg))
                       true? [{:href "uri" :rel "add"}]
                       true? [{:href "uri" :rel "add"} {:href "uri" :rel "delete"}]
                       false? []))

(deftest check-properties
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.common/properties arg))
                       true? {:a "ok"}
                       true? {:a "ok" :b "ok"}
                       true? {"a" "ok"}
                       true? {"a" "ok" "b" "ok"}
                       false? {}
                       false? {1 "ok"}
                       false? {"ok" 1}
                       false? [:bad "bad"]))

(def valid-acl {:owner {:principal "me" :type "USER"}})

(deftest check-AccessControlId
  (let [id {:principal "ADMIN", :type "ROLE"}]
    (are [expect-fn arg] (expect-fn (s/valid? :cimi.acl/owner arg))
                         true? id
                         false? (assoc id :bad "MODIFY")
                         false? (dissoc id :principal)
                         false? (dissoc id :type)
                         false? (assoc id :type "BAD"))))

(deftest check-rule
  (let [rule {:principal "ADMIN", :type "ROLE", :right "VIEW"}]
    (are [expect-fn arg] (expect-fn (s/valid? :cimi.acl/rule arg))
                         true? rule
                         true? (assoc rule :right "MODIFY")
                         true? (assoc rule :right "ALL")
                         false? (assoc rule :right "BAD")
                         false? (dissoc rule :right))))

(deftest check-rules
  (let [rules [{:principal "ADMIN", :type "ROLE", :right "VIEW"}
               {:principal "ALPHA", :type "USER", :right "ALL"}]]
    (are [expect-fn arg] (expect-fn (s/valid? :cimi.acl/rules arg))
                         true? rules
                         true? (next rules)
                         false? (nnext rules)
                         false? (cons 1 rules))))

(deftest check-acl
  (let [acl {:owner {:principal "::ADMIN"
                     :type      "ROLE"}
             :rules [{:principal ":group1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "group2"
                      :type      "ROLE"
                      :right     "MODIFY"}]}]
    (are [expect-fn arg] (expect-fn (s/valid? :cimi.acl/acl arg))
                         true? acl
                         true? (dissoc acl :rules)
                         false? (assoc acl :rules [])
                         false? (assoc acl :owner "")
                         false? (assoc acl :bad "BAD"))))

(s/def :cimi.test/common-attrs (apply t/only-keys (t/common-attrs)))

(deftest check-common-attrs
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
    (are [expect-fn arg] (expect-fn (s/valid? :cimi.test/common-attrs arg))
                         true? minimal
                         false? (dissoc minimal :id)
                         false? (dissoc minimal :resourceURI)
                         false? (dissoc minimal :created)
                         false? (dissoc minimal :updated)
                         true? maximal
                         true? (dissoc maximal :name)
                         true? (dissoc maximal :description)
                         true? (dissoc maximal :properties)
                         false? (assoc maximal :bad "BAD"))))

(deftest check-parameter-type
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.desc/type arg))
                       true? "string"
                       true? "boolean"
                       true? "int"
                       true? "float"
                       true? "timestamp"
                       true? "enum"
                       true? "map"
                       true? "list"
                       false? "unknown"))

(deftest check-parameter-desd-and-resource-desc
  (let [valid-desc {:displayName "ID"
                    :category    "common"
                    :description "unique resource identifier"
                    :type        "enum"
                    :mandatory   true
                    :readOnly    true
                    :order       0
                    :enum        ["a" "b" "c"]}
        resource-desc {:identifier valid-desc
                       :other      valid-desc
                       :acl        valid-acl}]

    (are [expect-fn arg] (expect-fn (s/valid? :cimi.desc/parameter-description arg))
                         true? valid-desc
                         true? (dissoc valid-desc :category)
                         true? (dissoc valid-desc :description)
                         true? (dissoc valid-desc :mandatory)
                         true? (dissoc valid-desc :readOnly)
                         true? (dissoc valid-desc :order)
                         true? (dissoc valid-desc :enum)
                         false? (assoc valid-desc :displayName 1)
                         false? (assoc valid-desc :category 1)
                         false? (assoc valid-desc :description 1)
                         false? (assoc valid-desc :type "unknown")
                         false? (assoc valid-desc :mandatory 1)
                         false? (assoc valid-desc :readOnly 1)
                         false? (assoc valid-desc :readOnly "1")
                         false? (assoc valid-desc :enum "1")
                         false? (assoc valid-desc :enum ["a" 1]))

    (are [expect-fn arg] (expect-fn (s/valid? :cimi.desc/resource-description arg))
                         true? resource-desc
                         false? (assoc resource-desc :another 1)
                         true? (assoc t/CommonParameterDescription :acl valid-acl))))
