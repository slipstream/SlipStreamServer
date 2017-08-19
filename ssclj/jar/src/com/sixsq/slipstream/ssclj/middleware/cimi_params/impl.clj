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
  "Calculates the value for the :size key in the CIMI parameters map. If the
  $size parameter is specified more than once, then the first valid long value
  will be used. If no valid values are specified, then this will return
  max-size. The value will be clipped to the range [0, max-size]."
  [{:strs [$size] :as params}]
  (-> $size
      utils/first-valid-long
      (or max-size)
      (max 0)
      (min max-size)))

(defn cimi-first
  "Calculates the value for the :first key in the CIMI parameters map. If a
   valid value isn't specified, this defaults to 1."
  [{:strs [$first] :as params}]
  (-> $first
      utils/first-valid-long
      (or 1)))

(defn cimi-last
  "Calculates the value for the :last key in the CIMI parameters map. If a
   valid value isn't specified, then the value is nil."
  [{:strs [$last] :as params}]
  (-> $last
      utils/first-valid-long))

(defn cimi-filter
  "Calculates the :filter key for the CIMI parameters map; the value is the
  AST resulting from the parsing of the complete filter. If the $filter
  parameter appears more than once, then the filters are combined with a
  logical AND. If the filter is invalid, then an exception is thrown."
  [{:strs [$filter] :as params}]
  (->> $filter
       utils/as-vector
       utils/wrap-join-with-and
       parser/parse-cimi-filter
       utils/throw-illegal-for-invalid-filter))

(defn cimi-expand
  "Calculates the value for the :expand key in the CIMI parameters map. The
  value will be :none if the attribute wasn't specified or if no valid values
  were given. If the wildcard is specified '*', then :all will be given as the
  value. In all other cases, a set of the specified attributes will be
  provided."
  [{:strs [$expand] :as params}]
  (->> $expand
       utils/as-vector
       (mapcat utils/comma-split)
       set
       utils/reduce-expand-set))

(defn cimi-select
  "Calculates the value for the :select key in the CIMI parameters map. The
  value will be nil if the $select key was not specified or if the wildcard
  value '*' is given. Otherwise a set of the desired keys (with 'resourceURI'
  added automatically) is returned."
  [{:strs [$select] :as params}]
  (some->> $select
           utils/as-vector
           (mapcat utils/comma-split)
           (cons "resourceURI")
           set
           utils/reduce-select-set))

(defn cimi-format
  "Calculates the value for the :format key in the CIMI parameters map. The
   processing of the $format parameter is more lenient than the CIMI
   specification in the following ways:

    - The first _acceptable_ value is used, rather than strictly the first.
    - Surrounding whitespace is removed from values before processing.

  In addition to the 'json' and 'xml' values in the specification, this also
  accepts 'edn'.

  Note that the specification states that this option must override any values
  provided in the HTTP Accept header. Consequently, the value must be checked
  before generating the output."
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
  "Calculates the value of the :orderby parameter in the CIMI parameters map.
  The value of the :orderby key will be an empty list if the parameter isn't
  specified or has no valid values. Otherwise it will contain a sequence of
  [attribute name, direction] tuples where the direction is either :asc
  (ascending) or :desc (descending)."
  [{:strs [$orderby] :as params}]
  (->> $orderby
       utils/as-vector
       (mapcat utils/comma-split)
       (map utils/orderby-clause)
       (remove nil?)))

(defn cimi-metric
  "Calculates the value of the :metric key for the CIMI parameters map. The
  value is a map where the keys are algorithm names (as keywords) and the
  values are the attribute names to which the algorithms should be applied."
  [{:strs [$metric] :as params}]
  (->> $metric
       utils/as-vector
       (mapcat utils/comma-split)
       (map utils/metric-clause)
       (remove nil?)
       (reduce utils/update-metric-map {})))
