(ns slipstream.async.launcher
  (:require
    [clojure.core.async :as async :refer [go timeout thread chan <! >! <!! alts!]]
    [slipstream.async.log :as log])
  (:import
    [com.sixsq.slipstream.connector Launcher]
    [com.sixsq.slipstream.persistence Run]
    [com.sixsq.slipstream.persistence User]
    [com.sixsq.slipstream.util Logger])
  (:gen-class
    :name slipstream.async.Launcher
    :methods [#^{:static true
                 :doc    "Takes: run user"}
  [launch [com.sixsq.slipstream.persistence.Run
           com.sixsq.slipstream.persistence.User] void]]))

(defn minutes-in-msecs
  [minutes]
  (* 1000 60 minutes))

(def launcher-chan-size 64)
(def number-of-readers 16)
(def timeout-processing-loop (minutes-in-msecs 1))
(def timeout-launch (minutes-in-msecs 15))

(def errors (atom 0))
(def requested (atom 0))
(def completed (atom 0))

; This is the channel for queuing all launch requests
(def launcher-chan (chan launcher-chan-size))

; Insert launch request
(defn >launch
  [run user]
  (swap! requested inc)
  (go (>! launcher-chan [run user])))

(defn launch!
  [run user]
  (let [ch (chan)]
    (go
      (let [[v c] (alts! [ch (timeout timeout-launch)])
            run-uuid (.getUuid run)]
        (if (nil? v)
          (do
            (log/log-error "Timeout launching run" run-uuid)
            (swap! errors inc)
            (Run/abort "Timeout launching run" run-uuid))
          (do
            (log/log-info "Launched run" run-uuid)
            (swap! completed inc)))))
    (go (>! ch (Launcher/launch run user)))))

; Start launch readers
(defn launch-readers
  []
  (log/log-debug "Starting " number-of-readers " readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[[run user] ch] (alts! [launcher-chan (timeout timeout-processing-loop)])]
          (if (nil? run)
            (log/log-debug "Launch reader " i " loop idle. Looping...")
            (try
              (launch! run user)
              (catch Exception e (log/log-error "caught exception: " (.getMessage e))))))))))

(defonce ^:dynamic *launch-processor* (launch-readers))

(defn -launch
  "Launch asyncronously all required VMs for the run"
  [run user]
  (>launch run user))
