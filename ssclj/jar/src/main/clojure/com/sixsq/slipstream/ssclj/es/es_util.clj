(ns com.sixsq.slipstream.ssclj.es.es-util
  (:require
    [me.raynes.fs :as fs]
    ;[sixsq.server.acl.acl :as acl]
    ;[sixsq.server.db.es-pagination :as pg]
    ;[sixsq.server.db.es-order :as od]
    ;[sixsq.server.db.es-filter :as ef]
    ;[clojure.data.json :as json]
    ;[clojure.java.io :as io]

    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.es.es-pagination :as pg]
    [clojure.java.io :as io])
  (:import
    [org.elasticsearch.node NodeBuilder Node]
    [org.elasticsearch.common.settings Settings]
    [org.elasticsearch.common.unit TimeValue]
    [org.elasticsearch.cluster.health ClusterHealthStatus]
    [java.util UUID]
    [org.elasticsearch.client Client]
    [org.elasticsearch.action.search SearchType]
    (org.elasticsearch.action.bulk BulkRequestBuilder)
    (org.elasticsearch.action ActionRequestBuilder)))

(defn json->edn [json]
  (when json (json/read-str json :key-fn keyword)))

(defn edn->json [edn]
  (json/write-str edn))

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
  (-> "mapping-not-analyzed.json"
      io/resource
      slurp))

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

(defn create-index
  [^Client client index-name]
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

(defn- throw-if-not-green [status]
  (or (= ClusterHealthStatus/GREEN status)
      (throw (ex-info "status is not GREEN" {:status (str status)}))))

(defn node-client [^Node node]
  (when node (.client node)))

(defn wait-for-cluster
  [^Client client]
  (let [status (.. (cluster-health client [])
                   (getStatus))]
    (throw-if-not-green status)))

(defn wait-for-index
  [^Client client index]
  (let [status (.. (cluster-health client [index])
                   (getIndices)
                   (get index)
                   (getStatus))]
    (throw-if-not-green status)))