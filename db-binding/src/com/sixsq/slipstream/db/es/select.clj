(ns com.sixsq.slipstream.db.es.select
  (:import
    (org.elasticsearch.action.search SearchRequestBuilder)))

(defn add-selected-keys
  "Adds the list of keys to select from the returned documents."
  [^SearchRequestBuilder request-builder {:keys [select] :as cimi-params}]
  (when select
    (let [^"[Ljava.lang.String;" includes (into-array String (conj select "acl"))]
      (.setFetchSource request-builder includes nil)))
  request-builder)
