(ns com.sixsq.slipstream.ssclj.usage.assembler
 (:require 
  [clj-time.core :as time]
  [com.sixsq.slipstream.ssclj.usage.utils :as u]))

;;
;; Computes for a given (user/cloud), timeframe (start/end) and dimension the actual usage on that dimension
;; dimension can be for example :nb-cpu, :memory-GB, ...
;;
;; 
;; Some cases 
;; ..0-|-------|-1
;; ..0-|---1...|..
;; ....|..0--1.|..
;; ....|..0----|1.
;; ....|.01..01|..
;;
;; Wording
;; 
;; a usage-event represents the start or end of the usage of some dimensions for a given id (user/cloud)
;; a block-usage is a pair of usage-event (start/end), special cases when no start or end 

(defn alternated?     [coll] (= (count coll) (count (partition-by :event coll))))
(defn first-is-start? [coll] (= :start (:event (first coll))))

(defn same-id-or-nil? [[u1 u2]] 
  (or 
    (nil? u2)
    (= (:id u1) (:id u2))))

(defn start-end?      
  [[u1 u2]] 
  (and 
    (= :start (:event u1)) 
    (or (nil? u2) (= :end (:event u2)))))

(defn extract-time   
  [usage-event]
  (if (nil? usage-event)
    nil
    (:timestamp usage-event)))

(defn build-block 
  [[u1 u2]]
  (u/check same-id-or-nil? [u1 u2] "Block can not be built with different id")
  (u/check start-end? [u1 u2] "Block must contain a start then an end")
  (u/check u/start-before-end? [(extract-time u1) (extract-time u2)] "Block must be ordered")

  { :id (:id u1)    
    :start-timestamp (:timestamp u1)
    :end-timestamp (:timestamp u2)
    :dimensions (:dimensions u1)})

(defn check-alternates [usage-events]
  (u/check alternated? usage-events "Consecutive start or end are not allowed."))

(defn check-first-is-start [usage-events]
  (u/check first-is-start? usage-events "First usage event must be a start."))

(defn by-id-sorted
  [id usage-events]
  (->> usage-events
    (filter #(= id (:id %)))
    (sort-by :timestamp)))

(defn complete-with-nil-if-odd   
  [usage-events]
  (if (odd? (count usage-events))
    (conj usage-events nil)
    usage-events))

(defn build-blocks 
  [id usage-events]  
  (->> (by-id-sorted id usage-events)
    check-alternates
    check-first-is-start
    complete-with-nil-if-odd
    (partition 2)
    (map build-block)))
