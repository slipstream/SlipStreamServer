(ns slipstream.async.launcher
  (:import [com.sixsq.slipstream.connector Launcher])
  (:import [com.sixsq.slipstream.persistence Run])
  (:import [com.sixsq.slipstream.persistence User])
  (:import [com.sixsq.slipstream.util Logger])
  (:require [clojure.core.async :as async :refer :all])
  (:gen-class
    :name slipstream.async.Launcher
    :methods [#^{:static true 
                 :doc "Takes: run user"}
                [launch [com.sixsq.slipstream.persistence.Run
                         com.sixsq.slipstream.persistence.User] void]]))

(defn minutes-in-msecs
  [minutes]
  (* 1000 60 minutes))

(def launcher-chan-size 1000)
(def number-of-readers 100)
(def timeout-processing-loop (minutes-in-msecs 1))
(def timeout-launch (minutes-in-msecs 5))

(def errors (atom 0))
(def requested (atom 0))
(def completed (atom 0))

(defn log-info
  [& msgs]
  (Logger/info (apply str msgs)))

(defn log-error
  [& msgs]
  (Logger/severe (apply str msgs)))

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
      (let [[v c] (alts! [ch (timeout timeout-launch)])]
        (if (nil? v)
          (do
            (log-error "Oops... timeout")
            (swap! errors inc))
          (do
            (log-info "Launched!")
            (swap! completed inc)))))
    (go (>! ch (Launcher/launch run user)))))

; Start launch readers
(defn launch-readers
  []
  (log-info "Starting " number-of-readers " readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[v ch] (alts! [launcher-chan (timeout timeout-processing-loop)])]
          (if (nil? v)
            (log-info "Reader " i " loop idle. Looping...")
            (do
              (swap! completed inc)
              (launch! (first v) (second v)))))))))

(defonce ^:dynamic *launch-processor* (launch-readers))

(defn -launch
  "Launch asyncronously all required VMs for the run"
  [run user]
  (>launch run user))
