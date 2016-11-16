(ns com.sixsq.slipstream.ssclj.migrate.rename-usage-summaries
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.impl :as db])
  (:import
    [org.elasticsearch.action.search SearchType]
    (org.elasticsearch.index.query QueryBuilders)))

(def bulk-size 100)

(defn- json->edn [json]
  (when json (json/read-str json :key-fn keyword)))

(defn- edn->json [edn]
  (json/write-str edn))

(defn- fetch
  [type]
  (->> (.. esb/*client*
          (prepareSearch (into-array String [esb/index-name]))
          (setTypes (into-array String [type]))
          (setSearchType SearchType/DEFAULT)
          (setQuery (QueryBuilders/matchAllQuery))
          (setSize 100000)
          (get))
      str
      json->edn
      :hits
      :hits
      (map :_source)))

(defn- id->uuid
  [id]
  (-> id
      (str/split #"/")
      second))

(defn- usage->uuid-summary-json
  [usage]
  (let [uuid (id->uuid (:id usage))
        usage-summary-json (-> usage
                               (set/rename-keys {:usage :usage-summary})
                               (assoc :id (str "usage-summary/" uuid))
                               edn->json)]
    [uuid usage-summary-json]))

(defn- new-usage-summaries
  []
  (let [result (map usage->uuid-summary-json (fetch "usage"))]
    (println (count result) " usages to copy.")
    result))

(defn- insert!
  [usage-summaries]
  (println "Inserting" (count usage-summaries) "usage-summaries")
  (esu/bulk-create esb/*client*
                   esb/index-name
                   "usage-summary"
                   usage-summaries))

(defn -main
  "Main function to copy usage resources to usage-summary"
  []
  (println "This script will copy usage resources to usage-summary resources by batches of" bulk-size "elements.")
  (println "Fetching existing usages...")

  (esb/set-client! (esb/create-client))
  (db/set-impl! (esb/get-instance))

  (->> (new-usage-summaries)
       (partition bulk-size bulk-size nil)
       (run! insert!)))