(ns com.sixsq.slipstream.ssclj.usage.record-keeper
  (:require 
    [clojure.string :only [join]]
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :refer :all :as jdbc] 
    [clojure.data.json :as json]      
    [korma.core :refer :all]
    [com.sixsq.slipstream.ssclj.api.korma-helper :as kh]
    [com.sixsq.slipstream.ssclj.usage.utils :as u])

  (:gen-class
    :name com.sixsq.slipstream.ssclj.usage.Record
    :methods [

    #^{:static true} [init            [] void]

    #^{:static true} [insertStart  [java.util.Map] void]
    #^{:static true} [insertEnd    [java.util.Map] void]]))

;;
;; Inserts in db usage records and retrieves them for a given interval.
;; 
;; 2 kind of insertion are possible:
;; * we store the start of a usage record (no end-timestamp yet)
;; * we "close" this usage record by assigning its end-timestamp.
;;
;; The usage-records is projected onto its metric dimensions, that means that
;; an original event on 3 dimensions will end up being stored as 3 rows.
;;
;; The records-for-interval retrieves the usage-records for a given interval.
;; (i.e records whose [start-end timestamps] intersect with the interval)
;;

(defn serialize
  [m]
  (with-out-str
   (json/pprint m)))
  
(defn anti-quote 
  [s] 
  (str \" s \"))

(defn col-desc 
  [[name type]]
  (str (anti-quote name) " " type))

(defn to-descs 
  [& name-types] 
  (->> 
    name-types
    (partition 2)
    (map col-desc)
    (clojure.string/join ",")))      

(defn columns-record  
  []
  (to-descs 
    "cloud_vm_instanceid"   "VARCHAR(100)"
    "user"                  "VARCHAR(100)"
    "cloud"                 "VARCHAR(100)"
    "start_timestamp"       "VARCHAR(30)"
    "end_timestamp"         "VARCHAR(30)"
    "metric_name"           "VARCHAR(100)"
    "metric_value"          "DOUBLE"))

(defn columns-summaries  
  []
  (to-descs     
    "user"                  "VARCHAR(100)"
    "cloud"                 "VARCHAR(100)"
    "start_timestamp"       "VARCHAR(30)"
    "end_timestamp"         "VARCHAR(30)"
    "usage"                 "VARCHAR(10000)"))

(def init-db
  (delay  
    (kh/korma-init)
    (log/info "Korma init done for insert namespace")    
    (jdbc/execute! kh/db-spec [(str "CREATE TABLE IF NOT EXISTS \"usage-records\" (" (columns-record) ")")])
    (jdbc/execute! kh/db-spec [(str "CREATE TABLE IF NOT EXISTS \"usage-summaries\" (" (columns-summaries) ")")])
    (log/info "Table created (if needed)")
    (defentity usage-records)
    (defentity usage-summaries)
    (select usage-records (limit 1))
    (log/info "Korma Entity 'usage-records' defined")))

(defn -init
  []
  @init-db)

(defn project   
  [usage-event metric]
  (-> usage-event
    (dissoc :metrics)
    (assoc :metric_name   (:name metric))
    (assoc :metric_value  (:value metric))))

(defn not-existing?
  [usage-event]
  (empty? (select usage-records (where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-event)}))))

(defn check-not-already-existing
  [usage-event]
  (u/check not-existing? usage-event (str "Usage record already inserted: " usage-event)))

(defn check-already-started
  [usage-event]
  (u/check (complement not-existing?) usage-event (str "Usage record not started: " usage-event)))  

(defn check-order
  [start end]
  (u/check u/start-before-end? [start end] "Invalid period"))

(defn -insertStart
  [usage-event]
  (log/info "Will persist usage event: " usage-event)
  (check-not-already-existing usage-event)
  (doseq [metric (:metrics usage-event)]    
    (let [usage-event-metric (project usage-event metric)]
      (log/debug "Will persist metric: " usage-event-metric)
      (insert usage-records (values usage-event-metric)))))

(defn -insertEnd
  [usage-event]
  (check-already-started usage-event)
  (update 
      usage-records 
      (set-fields {:end_timestamp (:end_timestamp usage-event)})
      (where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-event)})))

(defn insert-summary   
  [summary]
  (insert usage-summaries (values (update-in summary [:usage] serialize))))    

(defn records-for-interval
  [start end]
  (check-order start end)
  (select usage-records (where 
    (and 
      (or (= nil :end_timestamp) (>= :end_timestamp start))            
      (<= :start_timestamp end)))))
