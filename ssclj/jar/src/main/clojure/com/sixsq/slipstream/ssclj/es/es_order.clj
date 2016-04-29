(ns com.sixsq.slipstream.ssclj.es.es-order
  (:import
    (org.elasticsearch.action.search SearchRequestBuilder)
    (org.elasticsearch.search.sort SortOrder)))

(defn- direction->sortOrder
  [direction]
  (case direction
    :asc SortOrder/ASC
    :desc SortOrder/DESC
    :else (throw (IllegalArgumentException. (str "Invalid sorting direction: '" direction "', must be :asc or :desc")))))

(defn- add-sorter-from-cimi
  [^SearchRequestBuilder request [field-name direction]]
  (.addSort request field-name (direction->sortOrder direction)))

(defn add-sorters-from-cimi
  "Adds sorters to request with CIMI :orderby option."
  [^SearchRequestBuilder request cimi-params]
  (reduce add-sorter-from-cimi request (get-in cimi-params [:cimi-params :orderby])))

