(ns com.sixsq.slipstream.db.es.utils
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [environ.core :as env]
    [me.raynes.fs :as fs]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.es.pagination :as pg]
    [com.sixsq.slipstream.db.es.acl :as acl]
    [com.sixsq.slipstream.db.es.order :as order]
    [com.sixsq.slipstream.db.es.aggregate :as agg]
    [com.sixsq.slipstream.db.es.filter :as ef])
  (:import
    (org.elasticsearch.node Node)
    (org.elasticsearch.common.settings Settings)
    (org.elasticsearch.common.unit TimeValue)
    (org.elasticsearch.cluster.health ClusterHealthStatus)
    (org.elasticsearch.client Client)
    (org.elasticsearch.action.search SearchType SearchPhaseExecutionException SearchResponse)
    (org.elasticsearch.action.support WriteRequest$RefreshPolicy WriteRequest)
    (org.elasticsearch.action.bulk BulkRequestBuilder BulkResponse)
    (org.elasticsearch.action ActionRequestBuilder)
    (org.elasticsearch.action.admin.indices.delete DeleteIndexRequest)
    (org.elasticsearch.index.query QueryBuilders)
    (org.elasticsearch.index IndexNotFoundException)
    (org.elasticsearch.transport.client PreBuiltTransportClient)
    (org.elasticsearch.plugins Plugin)
    (org.elasticsearch.common.transport InetSocketTransportAddress)
    (java.net InetAddress)
    (org.elasticsearch.action.admin.indices.exists.indices IndicesExistsRequest)
    (java.util UUID)
    (org.elasticsearch.action.admin.indices.create CreateIndexResponse)))

(defn json->edn [json]
  (when json (json/read-str json :key-fn keyword)))

(defn edn->json [edn]
  (json/write-str edn))

;;
;; Elasticsearch implementations of CRUD actions
;;

(defn create
  [^Client client index type docid json]
  (.. client
      (prepareIndex index type docid)
      (setCreate true)
      (setRefreshPolicy WriteRequest$RefreshPolicy/IMMEDIATE)
      (setSource json)
      (get)
      (status)))

(defn read
  [^Client client index type docid]
  (.. client
      (prepareGet index type docid)
      (get)))

(defn update
  [^Client client index type docid json]
  (.. client
      (prepareUpdate index type docid)
      (setRefreshPolicy WriteRequest$RefreshPolicy/IMMEDIATE)
      (setDoc json)
      (get)))

(defn delete
  [^Client client index type docid]
  (.. client
      (prepareDelete index type docid)
      (get)))

(defn search
  ^SearchResponse [^Client client index type options]
  (try
    (let [query (-> options
                    ef/es-filter
                    (acl/and-acl options))

          [from size] (pg/from-size options)

          ^ActionRequestBuilder request (-> (.. client
                                                (prepareSearch (into-array String [index]))
                                                (setTypes (into-array String [(cu/de-camelcase type)]))
                                                (setSearchType SearchType/DEFAULT)
                                                (setQuery query)
                                                (setFrom from)
                                                (setSize size))
                                            (order/add-sorters options)
                                            (agg/add-aggregators options))]
      (.get request))
    (catch IndexNotFoundException infe
      (log/warn "index" index "not found, returning empty search result")
      [])
    (catch SearchPhaseExecutionException spee
      (log/warn "search failed:" (.getMessage spee) ", returning empty search result")
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
  ^BulkResponse [^Client client index type uuid-jsons]
  (let [bulk-request-builder (.. client
                                 (prepareBulk)
                                 (setRefreshPolicy WriteRequest$RefreshPolicy/WAIT_UNTIL))]
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
  "Creates a local elasticsearch node that holds data but cannot be accessed
   through the HTTP protocol."
  ([]
   (create-test-node (cu/random-uuid)))
  ([^String cluster-name]
   (let [home (str (fs/temp-dir "es-data-"))
         settings (.. (Settings/builder)
                      (put "http.enabled" false)
                      (put "node.data" true)
                      (put "cluster.name" cluster-name)
                      (put "transport.type" "local")
                      (put "path.home" home)
                      (put "logger.level" "INFO")
                      (build))]
     (.. (Node. ^Settings settings)
         (start)))))

(def ^:const mapping-not-analyzed
  (-> "com/sixsq/slipstream/db/es/mapping-not-analyzed.json"
      io/resource
      slurp))

(defn index-exists?
  [^Client client index-name]
  (.. client
      (admin)
      (indices)
      (exists (IndicesExistsRequest. (into-array String [index-name])))
      (get)
      (isExists)))

(defn create-index
  ^CreateIndexResponse [^Client client index-name]
  (log/info "creating index:" index-name)
  (let [settings (.. (Settings/builder)
                     (put "index.max_result_window" pg/max-result-window)
                     (put "index.number_of_shards" 3)
                     (put "index.number_of_replicas" 0)
                     (build))]
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
  "Creates a client connecting to an Elasticsearch instance. The 0-arity
  version takes the host and port from the environmental variables ES_HOST and
  ES_PORT. The 2-arity version takes these values as explicit parameters. If
  the host or port is nil, then \"localhost\" or \"9300\" are used,
  respectively."
  ([]
   (create-es-client (env/env :es-host) (env/env :es-port)))
  ([es-host es-port]
   (let [es-host (or es-host "localhost")
         es-port (or es-port "9300")]

     (log/info "creating elasticsearch client:" es-host es-port)
     (.. (new PreBuiltTransportClient ^Settings Settings/EMPTY [])
         (addTransportAddress (InetSocketTransportAddress. (InetAddress/getByName es-host)
                                                           (read-string es-port)))))))

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

(defn random-index-name
  "Creates a random value for an index name. The name is the string value of a
   random UUID, although that is an implementation detail."
  []
  (str (UUID/randomUUID)))

(defmacro with-es-test-client
  "Creates a new elasticsearch node, client, and test index, executes the body
   with `node`, `client`, and `index` vars bound to the values, and then
   reliably closes the node and client."
  [& body]
  `(with-open [~'node (create-test-node)
               ~'client (node-client ~'node)]
     (let [~'index (random-index-name)]
       (create-index ~'client ~'index)
       ~@body)))

