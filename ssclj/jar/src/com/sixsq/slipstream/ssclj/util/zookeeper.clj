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
    (zk/connect zk-endpoints)))

(def create-all (partial zk/create-all *client*))

(def create (partial zk/create *client*))

(def set-data (partial zk/set-data *client*))

(def get-data (partial zk/data *client*))

