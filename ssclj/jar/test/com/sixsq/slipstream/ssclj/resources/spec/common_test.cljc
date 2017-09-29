(ns com.sixsq.slipstream.ssclj.resources.spec.common-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as t]))

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
                       true? {:href "uri" :ok "value"}))

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

(deftest check-owner
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
  (let [acl {:owner {:principal "ADMIN"
                     :type      "ROLE"}
             :rules [{:principal "group1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "group2"
                      :type      "ROLE"
                      :right     "MODIFY"}]}]
    (are [expect-fn arg] (expect-fn (s/valid? :cimi.common/acl arg))
                         true? acl
                         true? (dissoc acl :rules)
                         false? (assoc acl :rules [])
                         false? (assoc acl :owner "")
                         false? (assoc acl :bad "BAD"))))

(s/def :cimi.test/common-attrs (su/only-keys-maps t/common-attrs))

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

(deftest check-zero-or-pos-int
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.core/zero-or-pos-int arg))
                       true? 0
                       true? 1
                       true? 1000
                       false? -1
                       false? -1000
                       false? 1.2
                       false? ""
                       false? {}))
