(ns com.sixsq.slipstream.ssclj.middleware.cimi-params-impl
  "Provides functions for the processing of CIMI query parameters.
  These functions provide the implementation of the ring middleware
  wrapper, but are not part of the public cimi-params middleware
  interface."

  (:require
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [com.sixsq.slipstream.ssclj.middleware.accepted-mime-types :as mime]
    [clojure.string :as s]
    [instaparse.core :as insta]))

(defn add-cimi-param
  "Adds the given key and value to the :cimi-params map in the
  ring request, creating the map if necessary."

  [{:keys [cimi-params] :or {:cimi-params {}} :as req} k v]
  (->> v
       (assoc cimi-params k)
       (assoc req :cimi-params)))

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
  (->> (get m k)
       (first-valid-long)))

(defn process-first-last
  "Adds the keys :first and :last to the :cimi-params map in the request.
  If these are not specified or have invalid values, then nil is provided
  as the value.  Otherwise the value is an integer value corresponding to
  the attribute value.

  This implementation takes into account the first *valid* value for these
  attributes, although the specification states that only the first value
  (valid or otherwise) should be used."

  [{:keys [params cimi-params] :or {:params {} :cimi-params {}} :as req}]
  (->> ["$first" "$last"]
       (map #(get-index params %))
       (zipmap [:first :last])
       (merge cimi-params)
       (assoc req :cimi-params)))

(defn filter-conjunction
  "A reduction function that will combine the given filter(s) with a
  logical AND expression."

  ([]
    [])
  ([a m]
   [:AndExpr a m]))

(defn process-filter
  "Adds the :filter key to the :cimi-params map in the request.  If
  the $filter parameter appears more than once, then the filters are
  combined with a logical AND.  Invalid filters are silently ignored."

  [{:keys [params] :or {:params {}} :as req}]
  (->> (get params "$filter")
       (as-vector)
       (map parser/parse-cimi-filter)
       (remove insta/failure?)
       (map second)
       (reduce filter-conjunction)
       (add-cimi-param req :filter)))

(defn comma-split
  "Split string on commas, optionally surrounded by whitespace.  All values
  have whitespace trimmed from both ends. Nil values and empty strings are
  removed from the output.  Passing nil returns an empty vector."
  [s]
  (if s
    (->> (s/split s #"\s*,\s*")
         (remove nil?)
         (map s/trim)
         (remove s/blank?))
    []))

(defn reduce-expand-set
  "If the attribute set contains the wildcard '*', then :all is returned
  Passing nil to the function will return nil."
  [key-set]
  (cond
    (contains? key-set "*") :all
    (empty? key-set) :none
    :else key-set))

(defn process-expand
  "Adds the :expand key to the :cimi-params map.  The value will be :none if
  the attribute wasn't specified or if no valid values were given.  If the
  wildcard is specified '*', then :all will be given as the value.  In all
  other cases, a set of the specified attributes will be provided.

  Whitespace around separators and around the attribute names are ignored
  and removed from the values."

  [{:keys [params] :or {:params {}} :as req}]
  (->> (get params "$expand")
       (as-vector)
       (mapcat comma-split)
       (set)
       (reduce-expand-set)
       (add-cimi-param req :expand)))

(defn reduce-select-set
  "If the attribute set contains the wildcard '*', then nil is returned
  since the effect of the wildcard is the same as if $select was not
  specified.  Passing nil to the function will also return nil."

  [key-set]
  (when-not (contains? key-set "*")
    key-set))

(defn process-select
  "Adds the :select key to the :cimi-params map.  The value will be nil if
  the $select key was not specified or if the wildcard value '*' is given.
  Otherwise a set of the desired keys is returned with 'resourceURI' added.

  Whitespace surrounding the attribute values is ignored."

  [{:keys [params] :or {:params {}} :as req}]
  (let [select (get params "$select")
        v (when select
            (->> select
                 (as-vector)
                 (mapcat comma-split)
                 (cons "resourceURI")
                 (set)
                 (reduce-select-set)))]
    (add-cimi-param req :select v)))

(defn process-format
  "Processes the $filter parameter(s) and adds the requested mime type to
  the :cimi-params map under the :format key.  The processing of the
  $filter parameter is more lenient than the CIMI specification in the
  following ways:

    - The first _acceptable_ value is used, rather than strictly the first.
    - Surrounding whitespace is removed from values before processsing.

  In addition to the 'json' and 'xml' values in the specification, this
  also accepts 'edn'.

  Note that the specification states that this option must override any
  values provided in the Accept header.  Consequently, the $format value
  must be checked before generating the output."

  [{:keys [params] :or {:params {}} :as req}]
  (->> (get params "$format")
       (as-vector)
       (filter string?)
       (map s/trim)
       (map s/lower-case)
       (filter mime/accepted-formats)
       (map #(get mime/accepted-mime-types %))
       (first)
       (add-cimi-param req :format)))

(defn orderby-clause
  "Splits an orderby clause value at the colon and then returns a
  two-value vector with the attribute value and either :asc or :desc.
  If the attribute value is blank or the value is not valid, then
  nil will be returned."
  [s]
  (let [[attr order] (->> (s/split s #"\s*:\s*")
                          (take 2)
                          (remove nil?)
                          (map s/trim))
        order (if (.equalsIgnoreCase "desc" order) :desc :asc)]
    (when-not (s/blank? attr)
      [attr order])))

(defn process-orderby
  "Adds the :orderby parameter to the :cimi-params map in the request.
  The value of the :orderby key will be an empty list if the parameter
  isn't specified or has no valid values.  Otherwise it will contain
  a sequence of attribute name, direction pairs, where the direction is
  either :asc (ascending) or :desc (descending)."

  [{:keys [params] :or {:params {}} :as req}]
  (->> (get params "$orderby")
       (as-vector)
       (mapcat comma-split)
       (map orderby-clause)
       (remove nil?)
       (add-cimi-param req :orderby)))
