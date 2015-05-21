(ns com.sixsq.slipstream.ssclj.resources.common.cimi-filter
  (:require
    [clojure.string :refer [split]]))

;;
;; Minimal implementation of CIMI resource filtering.
;; See dmtf.org/sites/default/files/standards/documents/DSP0263_1.0.1.pdf Section 4.1.6.1
;;

(defn- attribute-path
  [attribute-full-name]
  (map keyword (split attribute-full-name #"\.")))

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
  [attribute-full-name value]
  (fn [resource]
    (= value (attribute-value resource attribute-full-name))))

(defn- supress-wrap
  [s]
  (subs s 1 (dec (count s))))

(defn- wrapped?
  [s c]
  (and
    (>= (count s) 2)
    (.startsWith s c) (.endsWith s c)))

(defn- unwrap-quotes
  [s]
  (if (or (wrapped? s "\"") (wrapped? s "'"))
    (supress-wrap s)
    (throw (IllegalArgumentException. (str "Wrong format (must be simple or double quotted): "s)))))

(defn- split-attribute-value
  [s]
  (let [[attribute-full-name value] (split s #"=")]
    [attribute-full-name (unwrap-quotes value)]))

(defn cimi-filter
  [resources cimi-comp]
  (let [[attribute-full-name value] (split-attribute-value cimi-comp)]
    (filter (mk-pred-attribute-value attribute-full-name value) resources)))
