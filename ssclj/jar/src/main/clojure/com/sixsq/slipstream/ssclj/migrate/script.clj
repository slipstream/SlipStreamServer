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
    [com.sixsq.slipstream.ssclj.es.es-util :as esu]
    [com.sixsq.slipstream.ssclj.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.es.acl :as acl])
  (:gen-class))

;; Nb of documents stored per batch insert
(def batch-size 10)

(defn- id->uuid
  [id]
  (second (s/split id #"/")))

(defn- json->uuid-doc
  [json]
  [(id->uuid (:id (esu/json->edn json))) json])

(defn- bulk-store
  [type jsons]
  (esu/bulk-create esb/client esb/index type (map json->uuid-doc jsons)))

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
  (if-let [config-path (environ/env :config-path)]
    (-> config-path
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
  (let [current-keys (keys m)
        renamed-keys (map fix-case current-keys)]
    (set/rename-keys m (zipmap current-keys renamed-keys))))

(defn- select-count
  [type]
  (->> (kc/select :resources
                  (kc/where {:id [like (str type "/%")]})
                  (kc/fields ["count(*)"]))
       first
       :C1))

(defn- s->edn
  [s]
  (if (and (string? s )
           (.startsWith s "{"))
    (esu/json->edn s)
    s))

(defn- nested-json->edn
  [json]
  (into {}
        (map (fn [[k v]] [k (s->edn v)]) (esu/json->edn json))))

(defn- partition-batches
  [n]
  (->> batch-size
       (quot n)
       inc
       (* batch-size)
       range
       (partition batch-size)
       (map (juxt first last))))

(defn- migrate
  [resource]
  (println "Migrating " resource ", nb resources =" (select-count resource))
  (doseq [[start end] (partition-batches (select-count resource))]
    (println "migrating" start " -> " end)
    (let [jsons (->> (kc/select resources
                                (kc/where {:id [like (str resource "/%")]})
                                (kc/offset start)
                                (kc/limit end))
                     (map :data)
                     (map nested-json->edn)
                     (map acl/denormalize-acl)
                     (map fix-case-map)
                     (map esu/edn->json))]
      (bulk-store resource jsons))))

(defn -main
  "Main function to migrate resources from DB to Elastic Search"
  []
  (db/set-impl! (esb/get-instance))
  (esu/recreate-index esb/client esb/index)
  (do-korma-init)
  (println "This script will migrate resources from DB to Elastic Search")
  (let [resources ["usage" "usage-record"]]
    (run! migrate resources)
    ;; TODO
    (clojure.pprint/pprint (esu/dump esb/client esb/index "usage"))
    (clojure.pprint/pprint (esu/dump esb/client esb/index "usage-record"))
    ))



