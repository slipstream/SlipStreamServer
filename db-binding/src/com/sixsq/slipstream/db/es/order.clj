(ns com.sixsq.slipstream.db.es.order
  (:import
    (org.elasticsearch.action.search SearchRequestBuilder)
    (org.elasticsearch.search.sort SortOrder)))

(def sort-order {:asc  SortOrder/ASC
                 :desc SortOrder/DESC})

(defn direction->sort-order
  [direction]
  (or (sort-order direction)
      (throw (IllegalArgumentException. (str "invalid sorting direction '" direction "', must be :asc or :desc")))))

(defn add-sorter
  [^SearchRequestBuilder request [field-name direction]]
  (.addSort request field-name (direction->sort-order direction)))

(defn add-sorters
  "Adds sorters to request with CIMI :orderby option."
  [^SearchRequestBuilder request {{:keys [orderby]} :cimi-params :as options}]
  (reduce add-sorter request orderby))
