;;
;; To run this script:
;; java -cp .../ssclj-3.3-SNAPSHOT-standalone.jar com.sixsq.slipstream.ssclj.migrate.script
;;
(ns com.sixsq.slipstream.ssclj.migrate.script
  (:require
    [clojure.java.io :as io]
    [clojure.set :as set]
    [environ.core :as environ]
    [clojure.edn :as edn]
    [superstring.core :as s]
    [korma.core :as kc]
    [korma.db :as kdb]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.es.acl :as acl])
  (:gen-class))

;; Nb of documents stored per batch insert
(def batch-size 10000)

(defn- id->uuid
  [id]
  (second (s/split id #"/")))

(defn- json->uuid-doc
  [json]
  [(id->uuid (:id (esu/json->edn json))) json])

(defn- bulk-store
  [ty jsons]
  (esu/bulk-create esb/*client* esb/index-name ty (map json->uuid-doc jsons)))

(defn- find-resource
  [resource-path]
  (if-let [config-file (io/resource resource-path)]
    (do
      (println "================================================")
      (println (str "Will use " (.getPath config-file) " as config file"))
      config-file)
    (let [msg (str "Resource not found (must be in classpath): '" resource-path "'")]
      (println msg)
      (throw (IllegalArgumentException. msg)))))

(def default-db-spec
  {:classname   "org.hsqldb.jdbc.JDBCDriver"
   :subprotocol "hsqldb"
   :subname     "hsql://localhost:9001/ssclj"
   :make-pool?  true})

(def db-spec
  (if-let [config-name (environ/env :config-name)]
    (-> config-name
        find-resource
        slurp
        edn/read-string
        :api-db)
    (do
      (println (str "Using default db spec: " default-db-spec))
      default-db-spec)))

(defn- do-korma-init
  []
  (println "Will create korma database with db-spec")
  (println (u/map-multi-line db-spec))
  (kdb/defdb korma-api-db db-spec)
  (kc/defentity resources)
  (kc/defentity usage_records)
  (kc/defentity usage_summaries)

  (println "Korma database created"))

(defn- fix-case
  [k]
  (let [s (name k)
        first-letter (subs s 0 1)
        after (subs s 1)]
    (keyword
      (str first-letter (s/replace after #"\_" "-")))))

(defn- fix-case-map
  [m]
  (if (map? m)
    (let [current-keys (keys m)
          renamed-keys (map fix-case current-keys)]
      (set/rename-keys m (zipmap current-keys renamed-keys)))
    m))

(defn- fix-case-nested-map
  [m]
  (clojure.walk/prewalk fix-case-map m))

(defmulti select-count keyword)

(defn count-table
  [table-name]
  (->> (kc/select table-name (kc/fields ["count(*)"]))
       first
       :C1))

(defmethod select-count :default
  [type]
  (->> (kc/select :resources
                  (kc/where {:id [like (str type "/%")]})
                  (kc/fields ["count(*)"]))
       first
       :C1))

(defmethod select-count :usage-record
  [_]
  (count-table :usage_records))

(defmethod select-count :usage
  [_]
  (count-table :usage_summaries))

(defn- s->edn
  [s]
  (if (and (string? s ) (.startsWith s "{"))
    (esu/json->edn s)
    s))

(defn- nested-json->edn
  [json]
  (into {} (map (fn [[k v]] [k (s->edn v)]) (esu/json->edn json))))

(defn- nested->edn
  [m]
  (into {} (map (fn [[k v]] [k (s->edn v)]) m)))

(defn- partition-batches
  [n]
  (->> batch-size
       (quot n)
       inc
       (* batch-size)
       range
       (partition batch-size)
       (map (juxt first last))))

(defmulti data->jsons (comp keyword first))

(defmethod data->jsons :default
  [[resource start end]]
  (->> (kc/select :resources
                  (kc/where {:id [like (str resource "/%")]})
                  (kc/offset start)
                  (kc/limit end))
       (map :data)
       (map nested-json->edn)
       (map esb/force-admin-role-right-all)
       (map acl/denormalize-acl)
       (map fix-case-nested-map)
       (map esu/edn->json)))


(defmethod data->jsons :usage
  [[_ start end]]
  (->> (kc/select "usage_summaries"
                  (kc/offset start)
                  (kc/limit end))
       (map nested->edn)
       (map esb/force-admin-role-right-all)
       (map acl/denormalize-acl)
       (map fix-case-nested-map)
       (map esu/edn->json)))

(defn assoc-id
  [ty m]
  (assoc m :id (str ty "/" (u/random-uuid))))

(defmethod data->jsons :usage-record
  [[_ start end]]
  (->> (kc/select "usage_records"
                  (kc/offset start)
                  (kc/limit end))
       (map nested->edn)
       (map esb/force-admin-role-right-all)
       (map (partial assoc-id "usage-record"))
       (map acl/denormalize-acl)
       (map fix-case-nested-map)
       (map esu/edn->json)))

(defn- migrate
  [resource]
  (let [nb-resources (select-count resource)]
    (println "Migrating " resource ", nb resources =" nb-resources)
    (doseq [[start end] (partition-batches nb-resources)]
      (println "Migrating" resource start " -> " end)
      (bulk-store resource (data->jsons [resource start end]))))

  (println "First document" resource)
  (clojure.pprint/pprint (esu/dump esb/*client* esb/index-name resource)))

(defn -main
  "Main function to migrate resources from DB to Elastic Search"
  []
  (db/set-impl! (esb/get-instance))
  (esb/set-client! (esb/create-client))
  (esu/reset-index esb/*client* esb/index-name)
  (do-korma-init)
  (println "This script will migrate resources from DB to Elastic Search")
  (let [resources ["usage"
                   "usage-record"
                   "event"]]
    (run! migrate resources)))




