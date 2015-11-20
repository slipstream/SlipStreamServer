(ns com.sixsq.slipstream.ssclj.resources.common.cimi-filter
  (:refer-clojure :exclude [update])
  (:require
    [clojure.string :refer [split]]
    [instaparse.core :as insta]
    [instaparse.transform :as it]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]))

;;
;; partial implementation of cimi resource filtering.
;;
;; todo: more control on date comparison
;;
;; see dmtf.org/sites/default/files/standards/documents/dsp0263_1.0.1.pdf section 4.1.6.1
;;

(defn- remove-quotes    [s] (subs s 1 (dec (count s))))
(defn wrap-anti-quotes  [s] (str "\"" s "\""))
(defn- anti-quotes      [s] (-> s remove-quotes wrap-anti-quotes))

(defn to-int
  [x]
  (cond
    (number? x) (int x)
    (empty? x)  nil
    (string? x) (. Integer parseInt x)
    :else       nil))

(defn as-int
  [^String s]
  [:IntValue (. Integer parseInt s)])

(defn as-string
  [^String s]
  [:StringValue (remove-quotes s)])

(defn as-date
  [^String s]
  [:DateValue s])

(defn- attribute-path
  [attribute-full-name]
  (map keyword (split attribute-full-name #"/")))

(defn- attribute-value
  [resource attribute-full-name]
  (->>  attribute-full-name
        attribute-path
        (get-in resource)))

(defn mk-pred
  [attribute-full-name value compare-fn]
  (fn [resource]
    (let [actual-value (attribute-value resource attribute-full-name)]
      (compare-fn value actual-value))))

(defmulti mk-pred-attribute-value
  (fn [attribute-full-name op [type value]]
    [op type]))

(defmethod mk-pred-attribute-value ["=" :StringValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (= (str actual) value)))))

(defmethod mk-pred-attribute-value ["=" :DateValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [^String value ^String actual] (when actual (.startsWith actual value)))))

(defmethod mk-pred-attribute-value [">" :DateValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (> (compare actual value) 0)))))

(defmethod mk-pred-attribute-value ["<" :DateValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (< (compare actual value) 0)))))

(defmethod mk-pred-attribute-value ["=" :IntValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (= (to-int actual) value)))))

(defmethod mk-pred-attribute-value ["!=" :StringValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (not= (str actual) value)))))

(defmethod mk-pred-attribute-value ["!=" :IntValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (not= (int actual) value)))))

(defmethod mk-pred-attribute-value ["<" :StringValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (< (compare actual value) 0)))))

(defmethod mk-pred-attribute-value ["<" :IntValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (< (to-int actual) value)))))

(defmethod mk-pred-attribute-value [">" :StringValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (> (compare actual value) 0)))))

(defmethod mk-pred-attribute-value [">" :IntValue]
  [attribute-full-name op [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (when actual (> (to-int actual) value)))))

;;
;; fixme: this will not correctly handle clauses like "'a'=attribute".
;;
(defn handle-comp
  ([x]
    x)
  ([[_ a] [_ o] v]
    (mk-pred-attribute-value a o v)))

(defn or-preds
  [& preds]
  (fn [x]
    (some #(% x) preds)))

(defn and-preds
  [& preds]
  (apply every-pred preds))

;;
;; fixme: this does not handle booleans.
;;
(def ^:private transformations
  {:SingleQuoteString   as-string
   :DoubleQuoteString   as-string
   :DateValue           as-date
   :IntValue            as-int

   :Comp                handle-comp
   :AndExpr             and-preds
   :Filter              or-preds })

(defn sql-comp
  ([x]
   x)
  ;; FIXME use operator to build corresponding keyword operator
  ;; FIXME decouple from usage specific known columns
  ([[_ a] [_ o] v]
   (case o
     "="  (cond (some #{a} ["user" "cloud" "frequence"])         [:=     (keyword (str "u." a)) v]
                (some #{a} ["start_timestamp" "end_timestamp"])  [:like  (keyword (str "u." a)) (str v "%")])

     "<" (when  (some #{a} ["user" "cloud" "frequence" "start_timestamp" "end_timestamp"])
                [:<     (keyword (str "u." a)) v])
     ">" (when  (some #{a} ["user" "cloud" "frequence" "start_timestamp" "end_timestamp"])
                [:>     (keyword (str "u." a)) v]))))

(defn sql-or
  [& clauses]
  (cu/into-vec-without-nil :or clauses))

(defn sql-and
  [& clauses]
  (cu/into-vec-without-nil :and clauses))

;;
;; fixme: this does not handle booleans.
;;
(def ^:private sql-transformations
  {:SingleQuoteString   remove-quotes
   :DoubleQuoteString   anti-quotes
   :DateValue           identity
   :IntValue            to-int

   :Comp                sql-comp
   :AndExpr             sql-and
   :Filter              sql-or })

(defn to-predicates
  [tree]
  (it/transform transformations tree))

(defn- handle-failure
  [tree]
  (if (insta/failure? tree)
    (throw (IllegalArgumentException. (str "wrong format: " (insta/get-failure tree))))
    tree))

(defn sql-clauses
  [cimi-filter-tree]
  (when (seq cimi-filter-tree)
    (it/transform sql-transformations cimi-filter-tree)))

(defn cimi-filter-tree
  [cimi-filter-tree resources]
  (if (empty? (second cimi-filter-tree))
    resources
    (filter (to-predicates cimi-filter-tree) resources)))

(defn cimi-filter
  [resources cimi-filter-expression]
  (-> cimi-filter-expression
      parser/parse-cimi-filter
      handle-failure
      (cimi-filter-tree resources)))

