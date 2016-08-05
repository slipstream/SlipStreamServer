(ns slipstream.async.collector
  (:require
    [slipstream.async.log             :as log]
    [slipstream.async.metric-updator  :as updator]
    [clojure.core.async               :refer [go timeout thread chan sliding-buffer <! >! <!! alts!]])
  (:import
    [com.sixsq.slipstream.connector    Collector]
    [com.sixsq.slipstream.connector    ConnectorFactory]
    [com.sixsq.slipstream.persistence  User])
  (:gen-class
    :name slipstream.async.Collector
    :methods [#^{:static true :doc "Takes: run user"} [start [] void]]))

(defn seconds-in-msecs
  [seconds]
  (* 1000 seconds))

(defn msecs-in-seconds
  [msecs]
  (/ msecs 1000))

(def collector-chan-size            1024)
(def number-of-readers              32)
(def timeout-all-users-loop         (seconds-in-msecs 240))
(def timeout-online-loop            (seconds-in-msecs 10))
(def timeout-collect                (* 6 timeout-online-loop))
(def timeout-collect-reader-release (+ timeout-collect (seconds-in-msecs 2)))
(def timeout-processing-loop        (seconds-in-msecs 60))

(defn get-name
  [user]
  (.getName user))

(defn get-conn-inst-name
  [connector]
  (.getConnectorInstanceName connector))

(def busy (atom #{}))

(defn- mark-busy
 [current-busy v]
 (conj current-busy v))

(defn- mark-free
 [current-busy v]
 (disj current-busy v))

(defn- busy? [u c]
  (@busy [(get-name u) (get-conn-inst-name c)]))

(defn- busy!
  [u c]
  (swap! busy mark-busy [(get-name u) (get-conn-inst-name c)]))

(defn- free!
  [u c]
  (swap! busy mark-free [(get-name u) (get-conn-inst-name c)]))

(defn full?
  []
  (>= (count @busy) collector-chan-size))

(defn current-working-usage
  []
  (let [current-working (count @busy)
        ratio           (* 100.0 (/ current-working (float collector-chan-size)))]
    [current-working collector-chan-size ratio]))

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

; This is the channel for queuing the online user requests
(def online-collector-chan (chan (sliding-buffer collector-chan-size)))

; This is the channel for queuing all user requests
(def all-collector-chan (chan (sliding-buffer collector-chan-size)))

(defn- user-connector
 [user connector]
 (apply str "[" (get-name user) "/" (get-conn-inst-name connector) "] " (System/currentTimeMillis) " "))

(defn- build-msg
  [user connector elapsed & info]
  (apply str (user-connector user connector) " (" elapsed " ms) : " info))

(defn log-timeout
  [user connector elapsed]
  (log/log-error (build-msg user connector elapsed "Timed out when waiting for collecting vms")))

(defn log-failure
  [user connector elapsed]
  (log/log-error (build-msg user connector elapsed "Failed collecting VMs")))

(defn log-collected
  [user connector elapsed v]
  (log/log-info (build-msg user connector elapsed "Number of VMs collected = " v)))

(defn log-no-credentials
  [user connector elapsed]
  (log/log-debug (build-msg user connector elapsed "The user has no credentials for this Cloud")))

(defn update-metric!
  [user connector]
  (let [ch (chan 1)]
    (go
      (let [res (alts! [ch (timeout timeout-collect)])]
        (if (nil? res)
          (log/log-error  "Timeout updating metrics for user "  (get-name user))
          (log/log-debug   "Executed update metric request for " (get-name user)))))
    (go (>! ch (updator/update-metric user connector)))))

(defn collect!
  [user connector]
  (let [ch (chan 1) start-ts (System/currentTimeMillis)]
    (go
      (let [ [v _]    (alts! [ch (timeout timeout-collect-reader-release)])
             elapsed  (- (System/currentTimeMillis) start-ts) ]
        (free! user connector)
        (cond
          (nil? v)                            (log-timeout user connector elapsed)
          (= v Collector/NO_CREDENTIALS)      (log-no-credentials user connector elapsed)
          (= v Collector/EXCEPTION_OCCURED)   (log-failure user connector elapsed)
          :else (do
                  (log-collected user connector elapsed v)
                  (when (updator/metering-enabled?) (update-metric! user connector))))))
    (go (>! ch (Collector/collect user connector (msecs-in-seconds timeout-collect))))))

(def not-nil? (complement nil?))

; Start collector readers
(defn collect-readers
  [chan]
  (log/log-debug "Starting " number-of-readers " collector readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[[user connector] ch] (alts! [chan (timeout timeout-processing-loop)])]
          (when (not-nil? user)
            (try
              (log/log-debug "Will execute collect request for " (user-connector user connector))
              (collect! user connector)
              (catch Exception e (log/log-warn "caught exception executing collect request: " (.getMessage e))))))))))

(defonce ^:dynamic *online-collect-processor* (collect-readers online-collector-chan))
(defonce ^:dynamic *all-collect-processor* (collect-readers all-collector-chan))

(defn add-increasing-space
 [ucs step]
 (let [spaces (iterate #(+ step %) 0)]
   (map conj ucs spaces)))

(defn warn-channel-full
  [context u c]
  (log/log-error context
    ": the processing channel capacity(" collector-chan-size") is currently full, will *not* insert request for "
      (user-connector u c)))

(defn inform-avoiding
  [context u c]
  (log/log-info context ": avoiding inserting on " (user-connector u c) " being already processed."))

(defn inform-inserted
  [context u c t]
  (log/log-debug context ": inserted collect request for " (user-connector u c) " after timeout " t))

(defn inform-nothing-to-do
  [context]
  (log/log-debug context ": no users to collect."))

(defn show-current-channel-usage
  [context]
  (let [[current total ratio] (current-working-usage)]
    (log/log-info "indirect channel usage before inserting collect requests for " context ": " current " / " total " => " ratio " %")))

(defn log-initial-workspace
  [nu nc]
  (log/log-debug "Requested work on #" nu " users and #" nc " connectors. Initial workspace: " (* nu nc)))

(defn log-final-workspace
  [n]
  (log/log-debug "After filtering out users w/o connector creds. Final workspace: " n))

(defn log-spread
  [s t]
  (log/log-debug "Will spread " s " requests in " (msecs-in-seconds t) " s."))

(defn insert-collection-request-cond-spaced
  [chan u c t context]
  (go
    (<! (timeout t))
    (if-not (busy? u c)
      (do
        (busy! u c)
        (>! chan [u c])
        (inform-inserted context u c t))
      (inform-avoiding context u c))))

(defn compose-uc-pairs
  [users connectors]
  (for [u users c connectors :when (.isCredentialsSet c u)] [u c]))

(defn insert-collection-requests
  [users connectors chan time-to-spread context]
  (show-current-channel-usage context)
  (if (every? (comp pos? count) [connectors users])
    (let [_ (log-initial-workspace (count users) (count connectors))
          ucs (compose-uc-pairs users connectors)
          _ (log-final-workspace (count ucs))
          ucts (add-increasing-space ucs (int (/ time-to-spread (count ucs))))
          _ (log-spread (count ucts) time-to-spread)]
      (doseq [[u c t] ucts]
        (cond
          (full?) (warn-channel-full context u c)
          (busy? u c) (inform-avoiding context u c)
          :else (insert-collection-request-cond-spaced chan u c t context)))
      (inform-nothing-to-do context))))

; Start collector writers
(defn collect-writers
  []
  (go
    (while true
      (insert-collection-requests (online-users) (connectors) online-collector-chan timeout-online-loop "online users")
      (<! (timeout timeout-online-loop))))
  (go
    (while true
      (insert-collection-requests (users) (connectors) all-collector-chan timeout-all-users-loop "all users")
      (<! (timeout timeout-all-users-loop)))))

(defn -start
  []
  (collect-writers))
