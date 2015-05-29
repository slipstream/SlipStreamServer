(ns com.sixsq.slipstream.ssclj.resources.common.cimi-filter
  (:require
    [clojure.string                                           :refer [split]]
    [instaparse.core                                          :as insta]
    [instaparse.transform                                     :as it]
    [com.sixsq.slipstream.ssclj.filter.parser                 :as parser]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

;;
;; Partial Implementation of CIMI resource filtering.
;;
;; TODO: more control on date comparison
;;
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
    (let [actual-value (attribute-value resource attribute-full-name)]
      (case operator
        "="   (=    value actual-value)
        "!="  (not= value actual-value)
        "<"   (<  0 (compare value actual-value))
        ">"   (>  0 (compare value actual-value))))))

(defn- remove-quotes
  [s]
  (subs s 1 (dec (count s))))

(defn some-pred
  [preds]
  (fn [x]
    (some #(% x) preds)))

(def ^:private transformations
  {:SingleQuoteString   remove-quotes
   :DoubleQuoteString   remove-quotes
   :DateValue           identity
   :Comp                (fn[[_ a] [_ o] v]  (mk-pred-attribute-value a o v))
   :AndExpr             (fn [& comps]       (apply every-pred comps))
   :Filter              (fn [& preds]       (some-pred preds))})

(defn to-predicates
  [tree]
  (it/transform transformations tree))

(defn handle-failure
  [tree]
  (if (insta/failure? tree)
    (throw (IllegalArgumentException. (str "wrong format: " (insta/get-failure tree))))
    tree))

(defn cimi-filter
  [resources cimi-filter-expression]
  (-> cimi-filter-expression
      parser/parse-cimi-filter
      handle-failure
      to-predicates
      (filter resources)))

