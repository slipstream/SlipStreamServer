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

(defn- get-name
  [user]
  (.getName user))

(defn- get-conn-inst-name
  [connector]
  (.getConnectorInstanceName connector))

(defn- tuple-uc
  [u c]
  [(get-name u) (get-conn-inst-name c)])

(def busy (atom #{}))

(defn- mark-busy
 [current-busy v]
 (conj current-busy v))

(defn- mark-free
 [current-busy v]
 (disj current-busy v))

(defn- busy?
  [u c]
  (@busy (tuple-uc u c)))

(defn- busy!
  [u c]
  (swap! busy mark-busy (tuple-uc u c)))

(defn- free!
  [tuple-uc]
  (swap! busy mark-free tuple-uc))

(defn full?
  []
  (>= (count @busy) (* 2.0 collector-chan-size)))

(defn current-working-usage
  []
  (let [current-working (count @busy)
        total           (* 2 collector-chan-size) ; * 2 because there are 2 channels (offline and online users)
        ratio           (* 100.0 (/ current-working (float total)))]
    [current-working total ratio]))

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
 [tuple-uc]
 (apply format "[%s/%s]" tuple-uc))

(defn- build-msg
  [worker-id tuple-uc elapsed & info]
  (apply str worker-id " " (user-connector tuple-uc) " (" elapsed " ms) : " info))

(defn log-timeout
  [worker-id tuple-uc elapsed]
  (log/log-error (build-msg worker-id tuple-uc elapsed "Timed out when waiting for collecting vms")))

(defn log-failure
  [worker-id tuple-uc elapsed]
  (log/log-error (build-msg worker-id tuple-uc elapsed "Failed collecting VMs")))

(defn log-collected
  [worker-id tuple-uc elapsed v]
  (log/log-info (build-msg worker-id tuple-uc elapsed "Number of VMs collected = " v)))

(defn log-no-credentials
  [worker-id tuple-uc elapsed]
  (log/log-debug (build-msg worker-id tuple-uc elapsed "The user has no credentials for this Cloud")))

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
  [user connector worker-id]
  (let [ch        (chan 1)
        start-ts  (System/currentTimeMillis)]
    (go
      (let [[v _]     (alts! [ch (timeout timeout-collect-reader-release)])
            elapsed   (- (System/currentTimeMillis) start-ts)
            tuple-uc  (tuple-uc user connector)]
        (free! tuple-uc)
        (cond
          (nil? v)                            (log-timeout worker-id tuple-uc elapsed)
          (= v Collector/NO_CREDENTIALS)      (log-no-credentials worker-id tuple-uc elapsed)
          (= v Collector/EXCEPTION_OCCURRED)  (log-failure worker-id tuple-uc elapsed)
          :else (do
                  (log-collected worker-id tuple-uc elapsed v)
                  (when (updator/metering-enabled?) (update-metric! user connector))))))
    (go
      (log/log-debug "will collect for worker-id " (class worker-id) worker-id)
      (>! ch (Collector/collect user connector (msecs-in-seconds timeout-collect) worker-id)))))

(def not-nil? (complement nil?))

(defn worker-id
  [channel-name i]
  (str "Worker-" channel-name "/" i))

(defmacro forever
  "A macro that runs body in a while true loop, guarded by a try catch block.
  The body typically performs an action and waits for some time.
  Context parameter is used to report information when exception is caught.

  This is a macro (instead of regular function) because of a core.async limitation. Otherwise, we get:
  Uncaught Error: alts! used not in (go …) block
  "
  [context & body]
  `(try
    (while true
      (log/log-debug "Executing " ~context)
      ~@body)
    (catch Exception e#
      (log/log-warn ~context " caught exception : " (.getMessage e#)))))

(defn- go-collect
  "Launches in a go thread an infinite loop that collects VM for user/cloud"
  [chan worker-id]
  (go
    (log/log-debug "Go thread started for " worker-id)
    (forever (str "Collection for " worker-id)
             (let [[[user connector] _] (alts! [chan (timeout timeout-processing-loop)])]
               (when (not-nil? user)
                 (log/log-info worker-id " will execute collect request for " (user-connector (tuple-uc user connector)))
                 (collect! user connector worker-id))))))

; Start collector readers
(defn collect-readers
  [chan channel-name]
  (log/log-info channel-name ", will start " number-of-readers " collector readers.")
  (doseq [i (range number-of-readers)]
    (go-collect chan (worker-id channel-name i))))

(defonce ^:dynamic *online-collect-processor* (collect-readers online-collector-chan "online"))
(defonce ^:dynamic *all-collect-processor* (collect-readers all-collector-chan "offline"))

(defn add-increasing-space
 [ucs step]
 (let [spaces (iterate #(+ step %) 0)]
   (map conj ucs spaces)))

(defn warn-channel-full
  [context tuple-uc]
  (log/log-error context
    ": the processing channel capacity(" collector-chan-size") is currently full, will *not* insert request for "
      (user-connector tuple-uc)))

(defn inform-avoiding
  [context tuple-uc]
  (log/log-info context ": avoiding inserting on " (user-connector tuple-uc) " being already processed."))

(defn inform-inserted
  [context tuple-uc t]
  (log/log-debug context ": inserted collect request for " (user-connector tuple-uc) " after timeout " t))

(defn inform-nothing-to-do
  [context]
  (log/log-info context ": no users to collect."))

(defn show-current-channel-usage
  [context users]
  (let [[current total ratio] (current-working-usage)]
    (log/log-info (str "Channel usage before inserting collect requests for "
                       context ": " current " / " total " => " ratio " %, "
                       "(" (count users) " users)"))))

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
        (inform-inserted context (tuple-uc u c) t))
      (inform-avoiding context (tuple-uc u c)))))

(defn compose-uc-pairs
  [users connectors]
  (for [u users c connectors :when (.isCredentialsSet c u)] [u c]))

(defn insert-collection-requests
  [users connectors chan time-to-spread context]
  (show-current-channel-usage context users)
  (let [ucs (compose-uc-pairs users connectors)]
    (if-not (empty? ucs)
      (let [ucts (add-increasing-space ucs (int (/ time-to-spread (count ucs))))]
        (doseq [[u c t] ucts]
          (cond
            (full?)     (warn-channel-full context (tuple-uc u c))
            (busy? u c) (inform-avoiding context (tuple-uc u c))
            :else       (insert-collection-request-cond-spaced chan u c t context))))
      (inform-nothing-to-do context))))

; Start collector writers
(defn collect-writers
  []
  (go
    (forever "Writing online users collection"
             (insert-collection-requests (online-users) (connectors) online-collector-chan timeout-online-loop "online users")
             (<! (timeout timeout-online-loop))))
  (go
    (forever "Writing offline users collection"
             (insert-collection-requests (users) (connectors) all-collector-chan timeout-all-users-loop "offline users")
             (<! (timeout timeout-all-users-loop)))))

(defn -start
  []
  (collect-writers))
