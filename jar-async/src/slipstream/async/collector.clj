(ns slipstream.async.collector
  (:require [slipstream.async.log :as log])
  (:import [com.sixsq.slipstream.connector Collector])
  (:import [com.sixsq.slipstream.connector Connector])
  (:import [com.sixsq.slipstream.connector ConnectorFactory])
  (:import [com.sixsq.slipstream.persistence User])
  (:import [com.sixsq.slipstream.util Logger])
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
(def number-of-readers 100)
(def timeout-all-users-loop (seconds-in-msecs 120))
(def timeout-online-loop (seconds-in-msecs 10))
(def timeout-collect (seconds-in-msecs 5))
(def timeout-processing-loop (seconds-in-msecs 600))

(def errors (atom 0))
(def requested (atom 0))
(def completed (atom 0))

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

(defn collect!
  [user connector]
  (let [ch (chan)]
    (go
      (let [[v c] (alts! [ch (timeout timeout-collect)])]
        (if (nil? v)
          (do
            (swap! errors inc)
            (log/log-error
              "Timeout collecting vms for user "
              (.getName user)
              " on cloud "
              (.getConnectorInstanceName connector)))
          (do
            (swap! completed inc)))))
    (go (>! ch (Collector/collect user connector)))))

; Start collector readers
(defn collect-readers
  []
  (log/log-info "Starting " number-of-readers " collector readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[[user connector] ch] (alts! [collector-chan (timeout timeout-processing-loop)])]
          (if (nil? user)
            (log/log-info "Reader " i " loop idle. Looping...")
            (collect! user connector)))))))

(defonce ^:dynamic *collect-processor* (collect-readers))

(defn insert-requests
  [fetch-users-fn msg]
  (doseq [user (fetch-users-fn)
        connector (connectors)]
    (log/log-info
      msg
      " "
      (.getName user)
      " on cloud "
      (.getCloudServiceName connector))
    (go 
      (>! collector-chan [user connector]))))

; Start collector writers
(defn collect-writers
  []
  (thread
    (while true
      (<!! (timeout timeout-online-loop))
      (insert-requests online-users "Inserting request for collecting vms for online users")))
  (thread
    (while true
      (<!! (timeout timeout-online-loop))
      (insert-requests (users) "Inserting request for collecting vms for all users"))))

(defn -start
  []
  (collect-writers))
