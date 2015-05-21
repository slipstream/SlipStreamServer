(ns com.sixsq.slipstream.ssclj.resources.common.cimi-filter
  (:require
    [clojure.string                                           :refer [split]]
    [instaparse.core                                          :as insta]
    [instaparse.transform                                     :as it]
    [com.sixsq.slipstream.ssclj.filter.parser                 :as parser]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils  :as du]))

;;
;; Implementation of CIMI resource filtering.
;; See dmtf.org/sites/default/files/standards/documents/DSP0263_1.0.1.pdf Section 4.1.6.1
;;

(defn- attribute-path
  [attribute-full-name]
  (map keyword (split attribute-full-name #"/")))

(defn- check-present
  [attribute-full-name value]
  (if (nil? value)
    (throw (IllegalArgumentException. (str "Unknown attribute: "attribute-full-name)))
    value))

(defn- attribute-value
  [resource attribute-full-name]
  (->>  attribute-full-name
        attribute-path
        (get-in resource)
        (check-present attribute-full-name)))

(defn- mk-pred-attribute-value
  [attribute-full-name operator value]
  (fn [resource]
    (= value (attribute-value resource attribute-full-name))))

(defn- remove-quotes
  [s]
  (subs s 1 (dec (count s))))

(def ^:private transformations
  {:SingleQuoteString   remove-quotes
   :DoubleQuoteString   remove-quotes
   :Comp                (fn[[_ a] o v] (mk-pred-attribute-value a o v))
   :Filter              identity
   :AndExpr             identity})

(defn to-predicates
  [tree]
  (it/transform transformations tree))

(def assemble-predicates    identity)

(defn handle-failure
  [tree]
  (if (insta/failure? tree)
    (throw (IllegalArgumentException. (str "Wrong format: " tree)))
    tree))

(defn cimi-filter
  [resources cimi-filter-expression]

  (-> cimi-filter-expression
      parser/parse-cimi-filter
      handle-failure
      to-predicates
      ;assemble-predicates
      (filter resources)))

