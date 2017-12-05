(ns sixsq.slipstream.metering.spandex-utils
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.db.impl :as db]
    [qbits.spandex :as spandex])
  (:import (org.apache.http HttpHost)
           (org.elasticsearch.common.network NetworkAddress)
           (org.elasticsearch.client RestClient)))


(defn cluster-ready? [client]
  (try
    (let [{:keys [status] :as resp} (spandex/request client {:url    "/_cluster/health"
                                                             :method :get})
          cluster-status (-> resp :body :status)]
      (and (= 200 status) (not= "red" cluster-status)))
    (catch Exception _ false)))


(defn index-create [client index]
  (let [settings {:settings {:index {:number_of_shards   3
                                     :number_of_replicas 1}}}
        {:keys [status] :as resp} (spandex/request client {:url    index
                                                           :method :put
                                                           :body   settings})]
    (when-not (= 200 status)
      (throw (ex-info (str "create index failed: " status) resp)))))


(defn index-status [client index]
  (let [{:keys [status body] :as resp} (spandex/request client {:url    index
                                                                :method :get})]
    (when-not (= 200 status)
      (throw (ex-info (str "create index failed: " status) resp)))
    body))


(defn index-delete [client index]
  (let [{:keys [status] :as resp} (spandex/request client {:url    index
                                                           :method :delete})]
    (when-not (= 200 status)
      (throw (ex-info (str "delete index failed: " status) resp)))))


(defn index-add [client index {:keys [id] :as document}]
  (let [url (str/join "/" [index id])
        {:keys [status] :as resp} (spandex/request client {:url    url
                                                           :method :put
                                                           :body   document})]
    (when-not (= 201 status)
      (let [msg (str "adding document failed: " status ", " url)]
        (throw (ex-info msg resp))))))


(defn index-refresh [client index]
  (let [url (str index "/_refresh")
        {:keys [status] :as resp} (spandex/request client {:url    url
                                                           :method :post})]
    (when-not (= 200 status)
      (let [msg (str "refresh failed: " status ", " index)]
        (throw (ex-info msg resp))))))

(defn- addresses
  [nodeInfos]
  "Get a list of org.elasticsearch.common.transport.InetSocketTransportAddress for each NodeInfo
  where Http is enabled"
  (let [node-address (fn [node] (when (.getHttp node)
                                  (-> node
                                      (.getHttp)
                                      (.address)
                                      (.publishAddress)
                                      )))]
    (map node-address nodeInfos)))


(defn cli->rest
  "Take a node client and return a map with rest client and hosts "
  [client]
  (let [nodes (-> client
                  (.admin)
                  (.cluster)
                  (.prepareNodesInfo (into-array String []))
                  (.get)
                  (.getNodes))
        addresses (addresses nodes)
        host-address (fn [address] (HttpHost. (.getAddress address) (.getPort address) "http"))
        hosts (map host-address addresses)
        builder (RestClient/builder (into-array HttpHost hosts))]
    {:client (.build builder)
     :hosts  (vec (map str hosts))
     }))

(defn provide-test-client []
  (let [node (esu/create-test-node)]
    (esu/node-client node)))

(defn provide-mock-rest-client
  []
  (cli->rest (provide-test-client)))





