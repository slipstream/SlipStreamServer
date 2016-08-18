(ns com.sixsq.slipstream.db.es.acl
  (:require
    [superstring.core :as s]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.es.es-filter :as ef]))

(defn- rule-of-type?
  [type rule]
  (= type (:type rule)))

(def ^:private user-rule? (partial rule-of-type? "USER"))
(def ^:private role-rule? (partial rule-of-type? "ROLE"))
(def ^:const ^:private acl-users :_acl-users)
(def ^:const ^:private acl-roles :_acl-roles)

(derive ::modify ::view)
(derive ::all ::modify)
(def rights
  {"VIEW"   ::view
   "MODIFY" ::modify
   "ALL"    ::all
   ::view   ::view
   ::modify ::modify
   ::all    ::all})

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
  [query-builder options]
  (let [user-name-clause (when-let [user-name (:user-name options)] [[(name acl-users) user-name]])
        user-roles-clauses (map vector (repeat (name acl-roles)) (:user-roles options))
        acl-clauses (concat user-name-clause user-roles-clauses)
        acl-queries (map (fn [[field value]] (ef/term-query field value)) acl-clauses)
        query-acl (if (empty? acl-queries) query-no-result (ef/or-query acl-queries))]
    (ef/and-query [query-acl query-builder])))

(defn- capacity-for-role
  [action role]
  {:type "ROLE" :principal role :right action})

(defn- capacities-for-roles
  [action options]
  (map (partial capacity-for-role action) (:user-roles options)))

(defn- capacity-for-user
  [action options]
  {:type "USER" :principal (:user-name options) :right action})

(defn- capacities
  [options action]
  (cons (capacity-for-user action options)
        (capacities-for-roles action options)))

(defn type-principal
  [tpr]
  (select-keys tpr [:type :principal]))

(defn rule-applies?
  [rule capacity]
  (= (type-principal rule) (type-principal capacity)))

(defn rule-allows?
  [rule action]
  (isa? (rights (:right rule)) (rights action)))

(defn rule-allow-capacity?
  [rule capacity]
  (and (rule-applies? rule capacity) (rule-allows? rule (:right capacity))))

(defn anonymous-rule?
  [rule]
  (= (type-principal rule) {:type "ROLE" :principal "ANON"}))

(defn anonymous-allows?
  [rules action]
  (some #(and (anonymous-rule? %) (rule-allows? % action)) rules))

(defn check-can-do
  [data options action]
  (let [rules (rules-with-owner (:acl data))
        capacities (capacities options action)]
    (if (or (empty? (:acl data))
            (anonymous-allows? rules action)
            (some identity
                  (for [c capacities
                        r rules
                        :when (rule-allow-capacity? r c)] true)))
      data
      (throw (cu/ex-response (str "Unauthorized. ACL " (:acl data)
                                  " does not authorize [" (:user-name options) "/"
                                  (s/join "," (:user-roles options)) "] for action " action)
                             403 (:id data))))))
