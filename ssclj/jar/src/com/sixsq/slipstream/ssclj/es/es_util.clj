(ns com.sixsq.slipstream.ssclj.es.es-util
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [environ.core :as env]
    [me.raynes.fs :as fs]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.es.es-pagination :as pg]
    [com.sixsq.slipstream.ssclj.es.acl :as acl]
    [com.sixsq.slipstream.ssclj.es.es-order :as od]
    [com.sixsq.slipstream.ssclj.es.es-filter :as ef]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [clojure.string :as s])
  (:import
    [org.elasticsearch.node NodeBuilder Node]
    [org.elasticsearch.common.settings Settings]
    [org.elasticsearch.common.unit TimeValue]
    [org.elasticsearch.cluster.health ClusterHealthStatus]
    [org.elasticsearch.client Client]
    [org.elasticsearch.action.search SearchType SearchPhaseExecutionException]
    (org.elasticsearch.action.bulk BulkRequestBuilder)
    (org.elasticsearch.action ActionRequestBuilder)
    (org.elasticsearch.action.admin.indices.delete DeleteIndexRequest)
    (org.elasticsearch.index.query QueryBuilders)
    (org.elasticsearch.index IndexNotFoundException)
    (org.elasticsearch.client.transport TransportClient)
    (org.elasticsearch.common.transport InetSocketTransportAddress)
    (java.net InetAddress)
    (org.elasticsearch.action.admin.indices.exists.indices IndicesExistsRequest)))

(defn json->edn [json]
  (when json (json/read-str json :key-fn keyword)))

(defn edn->json [edn]
  (json/write-str edn))

;;
;; Elastic Search implementations of CRUD actions
;;

(defn create
  [^Client client index type docid json]
  (.. client
      (prepareIndex index type docid)
      (setCreate true)
      (setRefresh true)
      (setSource json)
      (get)
      (isCreated)))

(defn read
  [^Client client index type docid]
  (.. client
      (prepareGet index type docid)
      (get)))

(defn update
  [^Client client index type docid json]
  (.. client
      (prepareUpdate index type docid)
      (setRefresh true)
      (setDoc json)
      (get)))

(defn delete
  [^Client client index type docid]
  (.. client
      (prepareDelete index type docid)
      (get)))

(defn search
  [^Client client index type options]
  (try
    (let [query (-> options
                    ef/es-filter
                    (acl/and-acl options))

          [from size] (pg/from-size options)
          ^ActionRequestBuilder request (.. client
                                            (prepareSearch (into-array String [index]))
                                            (setTypes (into-array String [(u/de-camelcase type)]))
                                            (setSearchType SearchType/DEFAULT)
                                            (setQuery query)
                                            (setFrom from)
                                            (setSize size))

          request-with-sort (od/add-sorters-from-cimi request options)]
      (.get request-with-sort))
    (catch IndexNotFoundException infe
      (log/warn (str "Searching for index '" index "' not yet created, returns empty"))
      [])
    (catch SearchPhaseExecutionException spee
      (log/warn (str "Searching failed: " (.getMessage spee) ", returns empty"))
      [])))

;;
;; Convenience functions
;;

(defn- add-index
  [client index type ^BulkRequestBuilder bulk-request-builder [uuid json]]
  (let [new-index (.. client
                      (prepareIndex index type uuid)
                      (setCreate true)
                      (setSource json))]
    (.add bulk-request-builder new-index)))

(defn bulk-create
  [^Client client index type uuid-jsons]
  (let [bulk-request-builder  (.. client
                                  (prepareBulk)
                                  (setRefresh true))]
    (.. (reduce (partial add-index client index type) bulk-request-builder uuid-jsons)
        (get))))

(defn dump
  [^Client client index type]
  (-> (.. client
          (prepareSearch (into-array String [index]))
          (setTypes (into-array String [type]))
          (setSearchType SearchType/DEFAULT)
          (setQuery (QueryBuilders/matchAllQuery))
          (setSize 1)
          (get))
      str
      json->edn
      :hits
      :hits))

;;
;; Util functions
;;

(defn create-test-node
  "Creates a local elasticsearch node which holds data but
   cannot be accessed through the HTTP protocol."
  ([]
   (create-test-node (cu/random-uuid)))

  ([^String cluster-name]
   (let [home     (str (fs/temp-dir "es-data-"))
         settings (.. (Settings/settingsBuilder)
                      (put "http.enabled" false)
                      (put "node.data" true)
                      (put "cluster.name" cluster-name)
                      (put "path.home" home))]
     (.. (NodeBuilder/nodeBuilder)
         (settings settings)
         (local true)
         (node)))))

(def ^:const mapping-not-analyzed
  (-> "com/sixsq/slipstream/ssclj/es/mapping-not-analyzed.json"
      io/resource
      slurp))

(defn index-exists?
  [^Client client index-name]
  (let [exists? (.. client
                  (admin)
                  (indices)
                  (exists (IndicesExistsRequest. (into-array String [index-name])))
                  (get)
                  (isExists))]
      (log/info (str "Index "
                     index-name
                     (if exists? " already existing." " does not exist.")))
      exists?))

(defn create-index
  [^Client client index-name]
  (log/info (str "Creating index " index-name))
  (let [settings (.. (Settings/builder)
                     (put "index.max_result_window" pg/max-result-window)
                     (put "index.number_of_shards" 3)
                     (put "index.number_of_replicas" 0))]
    (.. client
        (admin)
        (indices)
        (prepareCreate index-name)
        (setSettings settings)
        (addMapping "_default_" mapping-not-analyzed)
        (get))))

(def ^:private ok-health-statuses #{ClusterHealthStatus/GREEN ClusterHealthStatus/YELLOW})

(defn- throw-if-cluster-not-healthy
  [status]
  (when-not (ok-health-statuses status)
      (throw (ex-info "status is not accepted" {:status (str status)}))))

(defn cluster-health
  [^Client client indexes]
  (.. client
      (admin)
      (cluster)
      (prepareHealth (into-array String indexes))
      (setWaitForGreenStatus)
      (setTimeout (TimeValue/timeValueSeconds 15))
      (get)))

(defn node-client [^Node node]
  (when node (.client node)))

(defn create-es-client
  "Creates a client connecting to an instance of Elastic Search
  Parameters (host and port) are taken from environment variables."
  []
  (let [es-host (env/env :es-host)
        es-port (env/env :es-port)]

    (when (some empty? [es-host es-port])
      (throw (Exception. "Please configure ES_HOST and ES_PORT properties (Elastic Search)")))

    (log/info (str "Will create Elastic Search client on " es-host ", port " es-port))
    (.. (TransportClient/builder)
        (build)
        (addTransportAddress (InetSocketTransportAddress. (InetAddress/getByName es-host)
                                                          (read-string es-port))))))

(defn create-test-es-client
  []
  (node-client (create-test-node)))

(defn wait-for-cluster
  [^Client client]
  (let [status (.. (cluster-health client [])
                   (getStatus))]
    (throw-if-cluster-not-healthy status)))

(defn wait-for-index
  [^Client client index]
  (let [status (.. (cluster-health client [index])
                   (getIndices)
                   (get index)
                   (getStatus))]
    (throw-if-cluster-not-healthy status)))

(defn reset-index
  [^Client client index-name]
  (when (index-exists? client index-name)
    (.. client
        (admin)
        (indices)
        (delete (DeleteIndexRequest. index-name))
        (get)))
  (create-index client index-name))
