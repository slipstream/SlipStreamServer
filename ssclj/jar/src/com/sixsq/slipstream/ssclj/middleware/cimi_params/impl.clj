(ns com.sixsq.slipstream.ssclj.middleware.cimi-params.impl
  "Provides functions that transform CIMI query parameters specified in a
   request into validated, reformatted values that facilitate processing at the
   database layer."

  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [com.sixsq.slipstream.ssclj.middleware.accepted-mime-types :as mime]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params.utils :as utils]))

(def ^:const max-size
  "The maximum number of resources that can be returned from a query."
  10000)

(defn cimi-size
  "Adds the :size key and value to the CIMI parameters map. If the $size
  parameter is specified more than once, then the first valid long value will
  be used. If no value is provided or all values are invalid, then this will
  return max-size. The value will be clipped to the range [0, max-size]."
  [{:strs [$size] :as params}]
  (-> $size
      utils/first-valid-long
      (or max-size)
      (max 0)
      (min max-size)))

(defn cimi-first
  "Adds the :first key and value to the CIMI parameters map. If a valid value
   isn't specified, this defaults to 1."
  [{:strs [$first] :as params}]
  (-> $first
      utils/first-valid-long
      (or 1)))

(defn cimi-last
  "Adds the :last key and value to the CIMI parameters map. If a valid value
   isn't specified, then the value is nil."
  [{:strs [$last] :as params}]
  (-> $last
      utils/first-valid-long))

(defn cimi-filter
  "Adds the :filter key to the :cimi-params map in the request.  If
  the $filter parameter appears more than once, then the filters are
  combined with a logical AND.  If the resulting filter is invalid,
  then an exception is thrown."
  [{:strs [$filter] :as params}]
  (->> $filter
       utils/as-vector
       utils/wrap-join-with-and
       parser/parse-cimi-filter
       utils/throw-illegal-for-invalid-filter))

(defn cimi-expand
  "Adds the :expand key to the :cimi-params map.  The value will be :none if
  the attribute wasn't specified or if no valid values were given.  If the
  wildcard is specified '*', then :all will be given as the value.  In all
  other cases, a set of the specified attributes will be provided.

  Whitespace around separators and around the attribute names are ignored
  and removed from the values."
  [{:strs [$expand] :as params}]
  (->> $expand
       utils/as-vector
       (mapcat utils/comma-split)
       set
       utils/reduce-expand-set))

(defn cimi-select
  "Adds the :select key to the :cimi-params map.  The value will be nil if
  the $select key was not specified or if the wildcard value '*' is given.
  Otherwise a set of the desired keys is returned with 'resourceURI' added.

  Whitespace surrounding the attribute values is ignored."
  [{:strs [$select] :as params}]
  (some->> $select
           utils/as-vector
           (mapcat utils/comma-split)
           (cons "resourceURI")
           set
           utils/reduce-select-set))

(defn cimi-format
  "Processes the $format parameter(s) and adds the requested mime type to
  the :cimi-params map under the :format key.  The processing of the
  $format parameter is more lenient than the CIMI specification in the
  following ways:

    - The first _acceptable_ value is used, rather than strictly the first.
    - Surrounding whitespace is removed from values before processing.

  In addition to the 'json' and 'xml' values in the specification, this
  also accepts 'edn'.

  Note that the specification states that this option must override any
  values provided in the Accept header.  Consequently, the $format value
  must be checked before generating the output."
  [{:strs [$format] :as params}]
  (->> $format
       utils/as-vector
       (filter string?)
       (map str/trim)
       (map str/lower-case)
       (filter mime/accepted-formats)
       (map #(get mime/accepted-mime-types %))
       first))

(defn cimi-orderby
  "Adds the :orderby parameter to the :cimi-params map in the request.
  The value of the :orderby key will be an empty list if the parameter
  isn't specified or has no valid values.  Otherwise it will contain
  a sequence of attribute name, direction pairs, where the direction is
  either :asc (ascending) or :desc (descending)."
  [{:strs [$orderby] :as params}]
  (->> $orderby
       utils/as-vector
       (mapcat utils/comma-split)
       (map utils/orderby-clause)
       (remove nil?)))

(defn cimi-metric
  "Adds the :metric key and value to the CIMI parameters map. The value is a
  map where the keys are algorithm names (as keywords) and the values are the
  attribute names to apply the algorithm to."
  [{:strs [$metric] :as params}]
  (->> $metric
       utils/as-vector
       (mapcat utils/comma-split)
       (map utils/metric-clause)
       (remove nil?)
       (reduce utils/update-metric-map {})))
