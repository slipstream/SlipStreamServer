(ns com.sixsq.slipstream.ssclj.usage.summary
 (:require 
  [clojure.set :as s]
  [clojure.tools.logging :as log]
  [clj-time.core :as t]
  [com.sixsq.slipstream.ssclj.usage.utils :as u]
  [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]))

;;
;; Cuts (truncate start or end timestamp) and aggregates usage records inside an interval.
;; Then summaries them.
;; Usage records must intersect with interval.
;; 

(defn intersect?
  [start-time end-time]
  (fn [usage-record]
    (and 
      (t/before? (u/to-time (:start_timestamp usage-record)) (u/to-time end-time))
      (or 
        (nil? (:end_timestamp usage-record)) 
        (t/after? (u/to-time (:end_timestamp usage-record)) (u/to-time start-time))))))

(defn filter-inside-interval
  [start-time end-time usage-records]
  (filter (intersect? start-time end-time) usage-records))

(defn shift-start
  [start]
  (fn [record]
    (let [record-start-time (:start_timestamp record)]  
      (assoc record :start_timestamp (u/max-time start record-start-time)))))

(defn shift-end
  [end]
  (fn [record]
    (let [record-end-time (:end_timestamp record)]
      (assoc record :end_timestamp (u/min-time end record-end-time)))))

(defn truncate   
  [start-time end-time records]
  (u/check u/start-before-end? [start-time end-time] (str "Invalid timeframe [ " start-time", "end-time" ]"))
  (->> records    
    (filter-inside-interval start-time end-time)
    (map (shift-start start-time))
    (map (shift-end end-time))))

(defn contribution   
  [record]
  (let [value (:metric_value record) 
        nb-minutes (-> (u/to-interval (:start_timestamp record) (:end_timestamp record))
                      t/in-seconds
                      (/ 60.0))]          
    (* value nb-minutes)))

(defn comsumption   
  [record]
  { :unit_minutes (contribution record)})

(defn sum-consumptions    
  [cons1 cons2]
  (update-in cons1 [:unit_minutes] #(+ % (:unit_minutes cons2))))

(defn merge-summary-record 
  [summary record]
  (let [record-metric (:metric_name record)
        record-comsumption (comsumption record)]
    (if-let [consumption-to-increase (get-in summary [:usage record-metric])]
      (assoc-in summary [:usage record-metric] (sum-consumptions consumption-to-increase record-comsumption))
      (assoc-in summary [:usage record-metric] record-comsumption))))

(defn empty-summary-for-record   
  [record start end]
  (-> record    
    (dissoc :cloud_vm_instanceid :metric_name :metric_value)
    (assoc :start_timestamp start)  
    (assoc :end_timestamp end)))

(defn merge-usage-user-cloud 
  [records-user-cloud start end]
  (let [summary-first-record (empty-summary-for-record (first records-user-cloud) start end)]
    (reduce merge-summary-record summary-first-record records-user-cloud)))

(defn user-cloud   
  [record]
  (select-keys record [:user :cloud]))

(defn summarize-per-user-cloud 
  [start-time end-time per-user-cloud]
  (for [records-user-cloud per-user-cloud]
    (merge-usage-user-cloud records-user-cloud start-time end-time)))    

(defn summarize-records
  [records start-time end-time]
  (->> records
    (truncate start-time end-time)
    (group-by user-cloud)
    vals
    (summarize-per-user-cloud start-time end-time)))    

(defn summarize
  [start-time end-time]  
  (summarize-records (rc/records-for-interval start-time end-time) start-time end-time))

(defn summarize-and-store
  [start-time end-time]   
  (let [summaries (summarize start-time end-time)]
    (log/info "Will persist" (count summaries) "summaries")
    (doseq [summary summaries]
      (rc/insert-summary summary))))

    
