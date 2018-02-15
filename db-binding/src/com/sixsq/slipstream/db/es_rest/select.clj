(ns com.sixsq.slipstream.db.es-rest.select)

(defn add-selected-keys
  "Adds the list of keys to select from the returned documents."
  [{:keys [select] :as cimi-params}]
  (when select
    {:_source (-> select
                  vec
                  (conj "acl"))}))
