(ns slipstream.async.garbage-collector
  (:require [clojure.core.async :as async :refer [go timeout thread chan sliding-buffer <! >! <!!]])
  (:require [slipstream.async.log :as log])
  (:require [slipstream.async.metric-updator :as updator])
  (:import [com.sixsq.slipstream.persistence Run])
  (:gen-class
    :name slipstream.async.GarbageCollector
    :methods [#^{:static true}
                [purge [] int]]))

(defn seconds-in-msecs
  [seconds]
  (* 1000 seconds))

(def collector-chan-size 4)
(def number-of-readers 2)
(def timeout-collect (seconds-in-msecs 5))
(def timeout-processing-loop (seconds-in-msecs 600))

(defn purge
  []
  (Run/purge))

; This is the channel for queuing all collect requests
(def collector-chan (chan (sliding-buffer collector-chan-size)))

(defn collect!
  []
  (let [ch (chan 1)]
    (go
      (let [[[no-of-purged] c] (alts! [ch (timeout timeout-collect)])]
        (if (nil? no-of-purged)
          (log/log-error
            "Timeout garbage collecting runs")
          (log/log-info
            (str "Purged " no-of-purged " runs")))))
    (go (>! ch (purge)))))

(def not-nil? (complement nil?))

; Start collector readers
(defn collect-readers
  []
  (log/log-info "Starting " number-of-readers " garbage collector readers...")
  (doseq [i (range number-of-readers)]
    (go
      (while true
        (let [[v ch] (alts! [collector-chan (timeout timeout-processing-loop)])]
          (if (not-nil? v)
            (collect!)))))))

(defonce ^:dynamic *collect-processor* (collect-readers))

(defn insert-collection-requests
  []
  (go (>! collector-chan)))

; Start collector writers
(defn collect-writers
  []
  (thread
    (while true
      (<!! (timeout timeout-processing-loop))
      (insert-collection-requests))))

(defn -start
  []
  (collect-writers))
