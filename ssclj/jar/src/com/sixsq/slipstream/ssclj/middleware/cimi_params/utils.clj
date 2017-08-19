(ns com.sixsq.slipstream.ssclj.middleware.cimi-params.utils
  "Provides functions for the processing of CIMI query parameters.
  These functions provide the implementation of the ring middleware
  wrapper, but are not part of the public cimi-params middleware
  interface."

  (:require
    [clojure.string :as str]
    [instaparse.core :as insta]))

(defn as-vector
  "Ensures that the given argument is a vector, coercing the
   given value if necessary.  Vectors and lists are returned
   as a vector.  Nil returns an empty list.  All other values
   will be wrapped into a 1-element vector."

  [arg]
  (cond
    (nil? arg) []
    (vector? arg) arg
    (list? arg) (vec arg)
    :else [arg]))

(defn as-long
  "Coerse the value into a long.  The value can either be a
   string or a long."

  [s]
  (let [s (str s)]
    (try
      (Long/parseLong ^String s)
      (catch NumberFormatException _
        nil))))

(defn first-valid-long
  "In a vector of strings or numbers, this extracts the first
   value that can be coerced into a valid long."

  [v]
  (->> v
       (as-vector)
       (map as-long)
       (remove nil?)
       (first)))

(defn get-index
  "Get the first valid long value for the given named attribute."
  [m k]
  (first-valid-long (get m k)))

(defn wrap-join-with-and
  "Wraps individual filters in parentheses and then combines them with
   a logical AND."
  [filters]
  (str/join " and " (map #(str "(" % ")") filters)))

(defn throw-illegal-for-invalid-filter
  [parse-result]
  (if (insta/failure? parse-result)
    (throw (r/ex-bad-CIMI-filter (insta/get-failure parse-result)))
    parse-result))

(defn comma-split
  "Split string on commas, optionally surrounded by whitespace.  All values
  have whitespace trimmed from both ends. Nil values and empty strings are
  removed from the output.  Passing nil returns an empty vector."
  [s]
  (if s
    (->> (str/split s #"\s*,\s*")
         (remove nil?)
         (map str/trim)
         (remove str/blank?))
    []))

;; FIXME: Should this return a function instead of :all and :none?
(defn reduce-expand-set
  "If the attribute set contains the wildcard '*', then :all is returned
  Passing nil to the function will return nil."
  [key-set]
  (cond
    (contains? key-set "*") :all
    (empty? key-set) :none
    :else key-set))

(defn reduce-select-set
  "If the attribute set contains the wildcard '*', then nil is returned
  since the effect of the wildcard is the same as if $select was not
  specified.  Passing nil to the function will also return nil."
  [key-set]
  (when-not (contains? key-set "*")
    key-set))

(defn orderby-clause
  "Splits an orderby clause value at the colon and then returns a
  two-value vector with the attribute value and either :asc or :desc.
  If the attribute value is blank or the value is not valid, then
  nil will be returned."
  [s]
  (let [[attr order] (->> s
                          (re-seq #"^(.+?)(?::(asc|desc))?$")
                          first
                          (drop 1)
                          (remove nil?)
                          (map str/trim))
        order (if (.equalsIgnoreCase "desc" order) :desc :asc)]
    (when-not (str/blank? attr)
      [attr order])))

(defn metric-clause
  "Splits a metric clause value at the last colon and then returns a two-value
  vector containing the attribute name and the name of the aggregation
  algorithm as a keyword. If the value is not valid (e.g. does not contain an
  algorithm name, then nil will be returned."
  [s]
  (let [[attr algo] (->> s
                         (re-matches #"(.+):([a-z]+)")
                         rest
                         (map str/trim))]
    (when-not (str/blank? attr)
      [(keyword algo) attr])))

(defn update-metric-map
  "Takes a metric map and a key-value pair. Updates the entry in the map
   corresponding to the key by appending the value to the end of the key's
   value."
  [m [k v]]
  (update m k #(conj (or % []) v)))
