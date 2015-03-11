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

(defn msecs-in-seconds
  [msecs]
  (/ msecs 1000))

(def busy (atom #{}))

(defn- mark-busy
 [current-busy v]
 (conj current-busy v))

(defn- free
 [current-busy v]
 (disj current-busy v))

(def collector-chan-size 512)
(def number-of-readers 32)
(def timeout-all-users-loop (seconds-in-msecs 240))
(def timeout-online-loop (seconds-in-msecs 10))
(def timeout-collect (* 6 timeout-online-loop))
(def timeout-collect-reader-release (+ timeout-collect (seconds-in-msecs 2)))
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

(defn- user-connector
 [user connector]
 (apply str "[" (get-name user) "/" (.getConnectorInstanceName connector) "] " (System/currentTimeMillis) " "))

(defn- build-msg
  [user connector elapsed & info]
  (apply str (user-connector user connector) " (" elapsed " ms) : " info))

(defn log-timeout
  [user connector elapsed]
  (log/log-error (build-msg user connector elapsed "Timed out in waiting for collecting vms")))

(defn log-failure
  [user connector elapsed]
  (log/log-error (build-msg user connector elapsed "Failed collecting vms")))

(defn log-collected
  [user connector elapsed v]
  (log/log-info (build-msg user connector elapsed "Number of VMs collected = " v)))

(defn log-no-credentials
  [user connector elapsed]
  (log/log-debug (build-msg user connector elapsed "The user has no credentials for this Cloud")))

(defn update-metric!
  [user]
  (let [ch (chan 1)]
    (go
      (let [res (alts! [ch (timeout timeout-collect)])]
        (if (nil? res)
          (log/log-error
            "Timeout updating metrics for user "
            (get-name user))
          (log/log-info (str "executed update-metric request for " (get-name user))))))
    (go (>! ch (updator/update user)))))

(defn collect!
  [user connector]
  (let [ch (chan 1) start-ts (System/currentTimeMillis)]
    (go
      (let [ [v _] (alts! [ch (timeout timeout-collect-reader-release)])
             elapsed (- (System/currentTimeMillis) start-ts) ]
        (swap! busy free [(get-name user) connector])
        (cond
          (nil? v) (log-timeout user connector elapsed)
          (= v Collector/NO_CREDENTIALS) (log-no-credentials user connector elapsed)
          (= v Collector/EXCEPTION_OCCURED) (log-failure user connector elapsed)
          :else (do
                  (log-collected user connector elapsed v)
                  (when (updator/metering-enabled?) (update-metric! user))))))
    (go (>! ch (Collector/collect user connector (msecs-in-seconds timeout-collect))))))

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
            (swap! busy mark-busy [(get-name user) connector])
            (try
              (log/log-info (str "executing collect request for " (get-name user) " and " (.getConnectorInstanceName connector)))
              (collect! user connector)
              (catch Exception e (log/log-warn "caught exception executing collect request: " (.getMessage e))))))))))

(defonce ^:dynamic *online-collect-processor* (collect-readers online-collector-chan))
(defonce ^:dynamic *all-collect-processor* (collect-readers all-collector-chan))

(defn check-channel-size
  [users connectors]
  (let [users-count (count users)
        connectors-count (count connectors)]
    (when (> (* users-count connectors-count) collector-chan-size)
      (log/log-error (str "The number of users x connectors: " users-count " x " connectors-count " exceeds the channel size: " collector-chan-size)))))

(defn add-increasing-space
 [ucs space]
 (let [spaces (iterate #(+ space %) 0)]
   (map (fn [[u c] t] [u c t]) ucs spaces)))

(defn insert-collection-requests
  [users chan time-to-spread context]
  (if (> (count users) 0)
    (let [connectors (connectors)
          ucs (for [u users c connectors] [u c])
          ucts (add-increasing-space ucs (int (/ time-to-spread (count ucs))))]
      (check-channel-size users connectors)
      (doseq [[u c t] ucts]
        (if-not (@busy [(get-name u) c])
          (go
            (<! (timeout t))
            (log/log-info "Inserting collect request for " (user-connector u c) " after timeout " t)
            (>! chan [u c]))
          (log/log-info "Avoiding working on " (user-connector u c) " being in a process." ))))
    (log/log-info "No users to collect for " context)))

; Start collector writers
(defn collect-writers
  []
  (go
    (while true
      (insert-collection-requests (online-users) online-collector-chan timeout-online-loop "online users")
      (<! (timeout timeout-online-loop))))
  (go
    (while true
      (insert-collection-requests (users) all-collector-chan timeout-all-users-loop "all users")
      (<! (timeout timeout-all-users-loop)))))

(defn -start
  []
  (collect-writers))