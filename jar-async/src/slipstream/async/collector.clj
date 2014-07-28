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

(def collector-chan-size 512)
(def number-of-readers 32)
(def timeout-all-users-loop (seconds-in-msecs 240))
(def timeout-online-loop (seconds-in-msecs 10))
(def timeout-collect (seconds-in-msecs 15))
(def timeout-processing-loop (seconds-in-msecs 60))

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

; This is the channel for queuing the online user requests
(def online-collector-chan (chan (sliding-buffer collector-chan-size)))

; This is the channel for queuing all user requests
(def all-collector-chan (chan (sliding-buffer collector-chan-size)))

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
      (let [res (alts! [ch (timeout timeout-collect)])]
        (if (nil? res)
          (log/log-error
            "Timeout updating metrics for user "
            (.getName user))
          (log/log-info (str "executed update-metric request for " (.getName user))))))
    (go (>! ch (updator/update user)))))

(def not-nil? (complement nil?))

; Start collector readers
(defn collect-readers
  [chan]
  (log/log-info "Starting " number-of-readers " collector readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[[user connector] ch] (alts! [chan (timeout timeout-processing-loop)])]
          (when (not-nil? user)
            (try
              (log/log-info (str "executing collect request for " (.getName user) " and " (.getConnectorInstanceName connector)))
              (collect! user connector)
              (update-metric! user)
              (catch Exception e (log/log-warn "caught exception executing collect request: " (.getMessage e))))))))))

(defonce ^:dynamic *online-collect-processor* (collect-readers online-collector-chan))
(defonce ^:dynamic *all-collect-processor* (collect-readers all-collector-chan))

(defn check-channel-size
  [users connectors]
  (let [users-count (count users)
        connectors-count (count connectors)]
    (when (> (* users-count connectors-count) collector-chan-size)
      (log/log-error (str "The number of users x connectors: " users-count " x " connectors-count " exceeds the channel size: " collector-chan-size)))))

(defn insert-collection-requests
  [users chan]
  (let [connectors (connectors)]
    (check-channel-size users connectors)
    (doseq [u users
            c connectors]
      (go 
        (>! chan [u c])))))

; Start collector writers
(defn collect-writers
  []
  (go
	  (while true
	    (<! (timeout timeout-online-loop))
	    (let [users (online-users)]
	      (insert-collection-requests users online-collector-chan))))
  (go
	  (while true
	    (<! (timeout timeout-all-users-loop))
	    (let [users (users)]
	      (insert-collection-requests users all-collector-chan)))))

(defn -start
  []
  (collect-writers))
