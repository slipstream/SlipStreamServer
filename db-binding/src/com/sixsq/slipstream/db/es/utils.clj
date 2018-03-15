(ns com.sixsq.slipstream.db.es.utils
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [environ.core :as env]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.es.pagination :as pg]
    [com.sixsq.slipstream.db.es.acl :as acl]
    [com.sixsq.slipstream.db.es.order :as order]
    [com.sixsq.slipstream.db.es.aggregation :as agg]
    [com.sixsq.slipstream.db.es.filter :as ef]
    [com.sixsq.slipstream.db.es.select :as select])
  (:import
    (java.net InetAddress)
    (java.util UUID)
    (org.elasticsearch.action.admin.indices.create CreateIndexResponse)
    (org.elasticsearch.action.admin.indices.delete DeleteIndexRequest)
    (org.elasticsearch.action.admin.indices.exists.indices IndicesExistsRequest)
    (org.elasticsearch.action.admin.indices.get GetIndexRequest)
    (org.elasticsearch.action.bulk BulkRequestBuilder BulkResponse)
    (org.elasticsearch.action.search SearchType SearchPhaseExecutionException SearchResponse SearchRequestBuilder)
    (org.elasticsearch.action.support WriteRequest$RefreshPolicy)
    (org.elasticsearch.common.settings Settings)
    (org.elasticsearch.common.transport InetSocketTransportAddress)
    (org.elasticsearch.common.unit TimeValue)
    (org.elasticsearch.client Client)
    (org.elasticsearch.cluster.health ClusterHealthStatus)
    (org.elasticsearch.index IndexNotFoundException)
    (org.elasticsearch.index.query QueryBuilders)
    (org.elasticsearch.node Node)
    (org.elasticsearch.transport.client PreBuiltTransportClient)))

(def ^:const max-result-window 200000)

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
  [^Client client index type docid {:keys [cimi-params] :as options}]
  (let [get-request-builder (.. client
                                (prepareGet index type docid))]
    (-> get-request-builder
        (select/add-selected-keys cimi-params)
        (.get))))

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

(defn add-query [^SearchRequestBuilder request-builder options]
  (.setQuery request-builder (-> options
                                 ef/es-filter
                                 (acl/and-acl options))))

(defn search
  "On success, returns the full response in edn format. On errors, it logs the
   error and returns an empty map."
  ^SearchResponse [^Client client index type {:keys [cimi-params] :as options}]
  (try
    (let [^SearchRequestBuilder request (-> (.. client
                                                (prepareSearch (into-array String [index]))
                                                (setTypes (into-array String [(cu/de-camelcase type)]))
                                                (setSearchType SearchType/DEFAULT))
                                            (add-query options)
                                            (select/add-selected-keys cimi-params)
                                            (pg/add-paging cimi-params)
                                            (order/add-sorters cimi-params)
                                            (agg/add-aggregators cimi-params))
          ^SearchResponse search-response (.get request)

          ;; FIXME: This status should be checked to ensure the query was successful.
          status (.getStatus (.status search-response))]

      (-> search-response str json->edn))
    (catch IndexNotFoundException infe
      (log/warn "index" index "not found, returning empty search result")
      {})
    (catch SearchPhaseExecutionException spee
      (log/warn "search failed on" index "for" type "with parameters" cimi-params
                ", returning empty search result; message:" (.getMessage spee))
      {})))

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
                     (put "index.max_result_window" max-result-window)
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

(defn get-all-indices
  "Returns array of index names as strings.  Useful with `refresh-all-indices`.
  Based on
  https://stackoverflow.com/questions/33134906/elasticsearch-find-all-indexes-using-the-java-client
  "
  [^Client client]
  (.. client
      (admin)
      (indices)
      (getIndex (GetIndexRequest.))
      (actionGet)
      (getIndices)))

(defn refresh-all-indices
  [^Client client]
  (.. client
      (admin)
      (indices)
      (prepareRefresh (get-all-indices client))
      (get)))

(defn refresh-indices
  [^Client client indexes]
  (.. client
      (admin)
      (indices)
      (prepareRefresh (into-array String indexes))
      (get)))

(defn refresh-index
  [^Client client index]
   (.. client
       (admin)
       (indices)
       (prepareRefresh (into-array String [index]))
       (get)))

(defn random-index-name
  "Creates a random value for an index name. The name is the string value of a
   random UUID, although that is an implementation detail."
  []
  (str (UUID/randomUUID)))
