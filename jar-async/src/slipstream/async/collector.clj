(ns slipstream.async.collector
  (:require [slipstream.async.log :as log])
  (:require [slipstream.async.metric-updator :as updator])
  (:import [com.sixsq.slipstream.connector Collector])
  (:import [com.sixsq.slipstream.connector Connector])
  (:import [com.sixsq.slipstream.connector ConnectorFactory])
  (:import [com.sixsq.slipstream.persistence User])
  (:require [clojure.core.async :as async :refer :all])
  (:gen-class
    :name slipstream.async.Collector
    :methods [#^{:static true 
                 :doc "Takes: run user"}
                [start [] void]]))

(defn seconds-in-msecs
  [seconds]
  (* 1000 seconds))

(def collector-chan-size 1000)
(def metrics-update-chan-size 1000)
(def number-of-readers 100)
(def timeout-all-users-loop (seconds-in-msecs 120))
(def timeout-online-loop (seconds-in-msecs 10))
(def timeout-collect (seconds-in-msecs 5))
(def timeout-processing-loop (seconds-in-msecs 600))

(defn get-value
  [entry]
  (.getValue entry))

(defn users
  []
  (User/list))

(defn connectors
  []
  (clojure.core/map get-value (ConnectorFactory/getConnectors)))

(defn online?
  [user]
  (true? (.isOnline user)))

(defn online-users
  []
  (filter online? (users)))

(defn get-name
  [user]
  (.getName user))

; This is the channel for queuing all collect requests
(def collector-chan (chan collector-chan-size))

; This is the channel for queuing all metric update requests
(def update-metric-chan (chan metrics-update-chan-size))

(defn collect!
  [user connector]
  (let [ch (chan 1)]
    (go
      (let [[v c] (alts! [ch (timeout timeout-collect)])]
        (when (nil? v)
          (log/log-error
            "Timeout collecting vms for user "
            (.getName user)
            " on cloud "
            (.getConnectorInstanceName connector)))))
    (go (>! ch (Collector/collect user connector)))))

(defn update-metric!
  [user]
  (let [ch (chan 1)]
    (go
      (let [[v c] (alts! [ch (timeout timeout-collect)])]
        (when (nil? v)
          (log/log-error
            "Timeout updating metrics for user "
            (.getName user)))))
    (go (>! ch (updator/update user)))))

; Start collector readers
(defn collect-readers
  []
  (log/log-info "Starting " number-of-readers " collector readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[[user connector] ch] (alts! [collector-chan (timeout timeout-processing-loop)])]
          (if (nil? user)
            (log/log-info "Collector reader " i " loop idle. Looping...")
            (collect! user connector)))))))

(defonce ^:dynamic *collect-processor* (collect-readers))

; Start metric update readers
(defn update-metric-readers
  []
  (log/log-info "Starting " number-of-readers " metric update readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[[user] ch] (alts! [update-metric-chan (timeout timeout-processing-loop)])]
          (if (nil? user)
            (log/log-info "Metric update reader " i " loop idle. Looping...")
            (update-metric! user)))))))

(defonce ^:dynamic *update-metric-processor* (update-metric-readers))

(defn insert-collection-requests
  [users msg]
  (doseq [user users
        connector (connectors)]
    (log/log-info
      msg
      " "
      (.getName user)
      " on cloud "
      (.getCloudServiceName connector))
    (go 
      (>! collector-chan [user connector]))))

(defn insert-update-metric-requests
  [users msg]
  (doseq [user users]
    (log/log-info
      msg
      " "
      (.getName user))
    (go 
      (>! update-metric-chan [user]))))

; Start collector writers
(defn collect-writers
  []
  (thread
    (while true
      (<!! (timeout timeout-online-loop))
      (let [users (online-users)]
        (insert-collection-requests users "Inserting request for collecting vms for online users")
        (insert-update-metric-requests users "Inserting request for metrics collection for online users"))))
  (thread
    (while true
      (<!! (timeout timeout-online-loop))
      (let [users (users)]
        (insert-collection-requests users "Inserting request for collecting vms for all users"))
        (insert-update-metric-requests users "Inserting request for metrics collecting for all users"))))

(defn -start
  []
  (collect-writers))
