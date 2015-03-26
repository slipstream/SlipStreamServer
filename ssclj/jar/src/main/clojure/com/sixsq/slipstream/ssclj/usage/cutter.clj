(ns com.sixsq.slipstream.ssclj.usage.cutter
 (:require 
  [clojure.tools.logging :as log]
  [clj-time.core :as time]
  [com.sixsq.slipstream.ssclj.usage.utils :as u]))

;;
;; Cuts and aggregates blocks inside a time frame
;; 

(defn starts-after?   
  [timestamp]
    (fn [block]
      (time/after? (:start-timestamp block) timestamp))) 

(defn ends-before?   
  [timestamp]
  (fn [block]
    (time/before? (:end-timestamp block) timestamp)))

(defn move-blocks-starts-after
  [start]
  (fn [block]
    (let [block-start-time (:start-timestamp block)]  
      (assoc block :start-timestamp (u/max-time start block-start-time)))))

(defn move-blocks-ends-before
  [end]
  (fn [block]
    (let [block-end-time (:end-timestamp block)]
      (assoc block :end-timestamp (u/min-time end block-end-time)))))

(defn cut   
  [blocks start-time end-time]
  (u/check u/start-before-end? [start-time end-time] "Invalid timeframe")

  (->> blocks    
    (remove (starts-after? end-time))
    (remove (ends-before? start-time))
    (map (move-blocks-starts-after start-time))
    (map (move-blocks-ends-before end-time))))
