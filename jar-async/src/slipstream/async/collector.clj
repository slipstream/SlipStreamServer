(ns slipstream.async.collector
  (:require [slipstream.async.log :as log])
  (:require [slipstream.async.metric-updator :as updator])
  (:import [com.sixsq.slipstream.connector Collector])
  (:import [com.sixsq.slipstream.connector Connector])
  (:import [com.sixsq.slipstream.connector ConnectorFactory])
  (:import [com.sixsq.slipstream.persistence User])
  (:require [clojure.core.async :as async :refer [go timeout thread chan sliding-buffer <! >! <!!]])
  (:gen-class
    :name slipstream.async.Collector
    :methods [#^{:static true 
                 :doc "Takes: run user"}
                [start [] void]]))

(defn seconds-in-msecs
  [seconds]
  (* 1000 seconds))

(def collector-chan-size 64)
(def metrics-update-chan-size 64)
(def number-of-readers 32)
(def timeout-all-users-loop (seconds-in-msecs 120))
(def timeout-online-loop (seconds-in-msecs 10))
(def timeout-collect (seconds-in-msecs 15))
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
(def collector-chan (chan (sliding-buffer collector-chan-size)))

; This is the channel for queuing all metric update requests
(def update-metric-chan (chan (sliding-buffer metrics-update-chan-size)))

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

(def not-nil? (complement nil?))

; Start collector readers
(defn collect-readers
  []
  (log/log-info "Starting " number-of-readers " collector readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[[user connector] ch] (alts! [collector-chan (timeout timeout-processing-loop)])]
          (if (nil? user)
            (log/log-info "Collect reader " i " loop idle. Looping...")
            (try
              (collect! user connector)
              (catch Exception e (log/log-error "caught exception: " (.getMessage e))))))))))

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
            (log/log-info "Metric reader " i " loop idle. Looping...")
            (try
              (update-metric! user)
              (catch Exception e (log/log-error "caught exception: " (.getMessage e))))))))))

(defonce ^:dynamic *update-metric-processor* (update-metric-readers))

(defn insert-collection-requests
  [users]
  (doseq [user users
        connector (connectors)]
    (go 
      (>! collector-chan [user connector]))))

(defn insert-update-metric-requests
  [users]
  (doseq [user users]
    (go 
      (>! update-metric-chan [user]))))

; Start collector writers
(defn collect-writers
  []
  (thread
    (while true
      (<!! (timeout timeout-online-loop))
      (let [users (online-users)]
        (insert-collection-requests users)
        (insert-update-metric-requests users))))
  (thread
    (while true
      (<!! (timeout timeout-all-users-loop))
      (let [users (users)]
        (insert-collection-requests users))
        (insert-update-metric-requests users))))

(defn -start
  []
  (collect-writers))
