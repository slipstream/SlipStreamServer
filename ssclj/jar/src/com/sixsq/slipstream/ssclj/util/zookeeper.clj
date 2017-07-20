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

(defn create-all [path & options]
  (apply zk/create-all *client* path options))

(defn create [path & options]
  (apply zk/create *client* path options))

(defn get-data [path & options]
  (let [result (apply zk/data *client* path options)
        data (:data result)
        value (when (-> data nil? not) (String. data))]
    (assoc result :data value)))

(defn get-version
  [path]
  (-> (get-data path) :stat :version))

(defn set-data [path value & options]
  (let [version (get-version path)
        data (.getBytes (str value) "UTF-8")]
    (apply zk/set-data *client* path data version options)))
