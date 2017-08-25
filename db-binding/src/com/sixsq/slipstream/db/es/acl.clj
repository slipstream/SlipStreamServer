(ns com.sixsq.slipstream.db.es.acl
  (:require
    [superstring.core :as s]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.es.filter :as ef]))

(defn- rule-of-type?
  [type rule]
  (= type (:type rule)))

(def ^:private user-rule? (partial rule-of-type? "USER"))
(def ^:private role-rule? (partial rule-of-type? "ROLE"))
(def ^:const ^:private acl-users :_acl-users)
(def ^:const ^:private acl-roles :_acl-roles)

(defn- rules-with-owner
  [acl]
  (let [owner-rule (some-> acl :owner (assoc :right "ALL"))]
    (->> acl :rules (cons owner-rule) (into #{}) vec)))

(defn- extract-by-type
  [acl type-pred]
  (->> (rules-with-owner acl) (filter type-pred) (map :principal)))

(defn- extract-users
  [acl]
  (extract-by-type acl user-rule?))

(defn- extract-roles
  [acl]
  (extract-by-type acl role-rule?))

(defn denormalize-acl
  "Denormalize doc by exploding :acl in :_acl-users and :_acl-roles
  in a form easily searchable by Elastic Search"
  [doc]
  (-> doc
      (assoc acl-users (-> doc :acl extract-users))
      (assoc acl-roles (-> doc :acl extract-roles))))

(defn normalize-acl
  "Dissoc denormalized field from doc"
  [doc]
  (dissoc doc acl-users acl-roles))

(def ^:private query-no-result (ef/term-query "id" ""))

(defn and-acl
  "Enriches query-builder by adding a clause on ACL (extracted from options)"
  [query-builder {:keys [user-name user-roles] :as options}]
  (let [user-name-clause (when user-name [[(name acl-users) user-name]])
        user-roles-clauses (map vector (repeat (name acl-roles)) user-roles)
        acl-clauses (concat user-name-clause user-roles-clauses)
        acl-queries (map (fn [[field value]] (ef/term-query field value)) acl-clauses)
        query-acl (if (empty? acl-queries) query-no-result (ef/or-query acl-queries))]
    (ef/and-query [query-acl query-builder])))
