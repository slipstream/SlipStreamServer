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

(defn- attribute-path
  [attribute-full-name]
  (map keyword (split attribute-full-name #"/")))

(defn- check-present
  [resource attribute-full-name value]
  (if (nil? value)
    (throw (IllegalArgumentException.
             (str "unknown attribute: "attribute-full-name " in " resource)))
    value))

(defn- attribute-value
  [resource attribute-full-name]
  (->>  attribute-full-name
        attribute-path
        (get-in resource)
        (check-present resource attribute-full-name)))

(defn mk-pred
  [attribute-full-name value compare-fn]
  (fn [resource]
    (let [actual-value (attribute-value resource attribute-full-name)]
      (compare-fn value actual-value))))

(defmulti mk-pred-equals? (fn [attribute-full-name [type value]] type))
(defmethod mk-pred-equals? :default
  [attribute-full-name [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (= value (str actual)))))

(defmulti mk-pred-lower? (fn [actual_value [type value]] type))
(defmethod mk-pred-lower? :default
  [attribute-full-name [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (< 0 (compare value actual)))))

(defmulti mk-pred-greater? (fn [actual_value [type value]] type))
(defmethod mk-pred-greater? :default
  [attribute-full-name [type value]]
  (mk-pred attribute-full-name
           value
           (fn [value actual] (> 0 (compare value actual)))))

;; (defn- mk-pred-attribute-value
;;   [attribute-full-name operator value]
;;   (fn [resource]
;;     (let [actual-value (attribute-value resource attribute-full-name)]
;;       (case operator
;;         "="
;;         (do
;;           (du/show value)
;;           (du/show actual-value)
;;           (= value actual-value))
;;
;;         "!="  (not= value actual-value)
;;         "<"   (<  0 (compare value actual-value))
;;         ">"   (>  0 (compare value actual-value))))))
;;

(defn- mk-pred-attribute-value
  [attribute-full-name operator value]
    (case operator
      "="     (mk-pred-equals? attribute-full-name value)
      "!="    (complement (mk-pred-equals? attribute-full-name value))

      "<"     (mk-pred-lower? attribute-full-name value)
      ">"     (mk-pred-greater? attribute-full-name value)
      ))

;;
;; fixme: this will not correctly handle clauses like "'a'=attribute".
;;
(defn handle-comp
  ([x]
    x)
  ([[_ a] [_ o] v]
    (mk-pred-attribute-value a o v)))


(defn- remove-quotes
  [s]
  (subs s 1 (dec (count s))))

(defn wrap-anti-quotes
  [s]
  (str "\"" s "\""))

(defn- anti-quotes
  [s]
  (-> s
      remove-quotes
      wrap-anti-quotes))

(defn to-int    [s] [:IntValue (. Integer parseInt s)])
(defn to-string [s] [:StringValue (remove-quotes s)])
(defn to-date   [s] [:DateValue s])

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
  {:SingleQuoteString   to-string
   :DoubleQuoteString   to-string
   :DateValue           to-date
   :IntValue            to-int

   :Comp                handle-comp
   :AndExpr             and-preds
   :Filter              or-preds })

(defn sql-comp
  ([x]
   x)
  ;; FIXME use operator to build corresponding keyword operator
  ;; FIXME decouple from usage specific known columns
  ([[_ a] [_ o] v]
   (when (some #{a} ["user" "cloud" "start_timestamp" "end_timestamp"])
    [:= (keyword (str "u." a)) v])))

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

