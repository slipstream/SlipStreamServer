(ns com.sixsq.slipstream.ssclj.util.zookeeper
  (:require
    [environ.core :as env]
    [clojure.tools.logging :as log]
    [zookeeper :as zk]))

(def ^:dynamic *client*)

(defn set-client!
  [client]
  (alter-var-root #'*client* (constantly client)))

(defn create-client
  "Creates a client connecting to an instance of Zookeeper
  Parameters (host and port) are taken from environment variables."
  []
  (let [zk-endpoints (or (env/env :zk-endpoints) "localhost:2181")]
    (log/info "creating zookeeper client:" zk-endpoints)
    (zk/connect zk-endpoints :timeout-msec 60000)))

(defn close-client! []
  (let [client *client*]
    (alter-var-root #'*client* (constantly nil))
    (zk/close client)))

(defn string-to-byte [value]
  (.getBytes (str value) "UTF-8"))

(defn create-all [path & options]
  (apply zk/create-all *client* path options))

(defn create [path & options]
  (apply zk/create *client* path options))

(defn get-znode [path & options]
  (let [result (apply zk/data *client* path options)
        data (:data result)
        value (when (-> data nil? not) (String. data))]
    (assoc result :data value)))

(defn get-data [path & options]
  (-> (apply get-znode path options)
      :data))

(defn get-stat [path & options]
  (-> (apply get-znode path options)
      :stat))

(defn get-version
  [path]
  (-> (get-znode path) :stat :version))

(defn set-data [path value & options]
  (let [version (get-version path)
        data (string-to-byte value)]
    (apply zk/set-data *client* path data version options)))

(defn exists [path & options]
  (apply zk/exists *client* path options))

(defn children [path & options]
  (apply zk/children *client* path options))

(defn delete-all [path & options]
  (apply zk/delete-all *client* path options))

(defn delete [path & options]
  (apply zk/delete *client* path options))

