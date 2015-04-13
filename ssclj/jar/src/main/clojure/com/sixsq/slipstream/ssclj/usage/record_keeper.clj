(ns com.sixsq.slipstream.ssclj.usage.record-keeper
  (:require 
    [clojure.string :only [join]]
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :refer :all :as jdbc]
    [korma.core :as kc]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.database.korma-helper :as kh]    
    [com.sixsq.slipstream.ssclj.database.ddl :as ddl]
    [com.sixsq.slipstream.ssclj.usage.utils :as u])
  (:gen-class
    :name com.sixsq.slipstream.usage.Record
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
  
(defonce ^:private columns-record 
  (ddl/columns 
    "cloud_vm_instanceid"   "VARCHAR(100)"
    "user"                  "VARCHAR(100)"
    "cloud"                 "VARCHAR(100)"
    "start_timestamp"       "VARCHAR(30)"
    "end_timestamp"         "VARCHAR(30)"
    "metric_name"           "VARCHAR(100)"
    "metric_value"          "FLOAT"))

(defonce ^:private columns-summaries    
  (ddl/columns     
    "id"                    "VARCHAR(100)"
    "acl"                   "VARCHAR(1000)"
    "user"                  "VARCHAR(100)"
    "cloud"                 "VARCHAR(100)"
    "start_timestamp"       "VARCHAR(30)"
    "end_timestamp"         "VARCHAR(30)"
    "usage"                 "VARCHAR(10000)"))

(def init-db
  (delay  
    (kh/korma-init)
    (log/info "Korma init done for insert namespace")    

    (ddl/create-table! "usage-records" columns-record)
    (ddl/create-table! "usage-summaries" columns-summaries)

    (ddl/create-index! "usage-records"   "IDX_TIMESTAMPS" "start_timestamp", "end_timestamp")
    (ddl/create-index! "usage-summaries" "IDX_TIMESTAMPS" "start_timestamp", "end_timestamp")

    (log/info "Table created (if needed)")
    (kc/defentity usage-records)
    (kc/defentity usage-summaries)
    (kc/select usage-records (kc/limit 1))
    (log/info "Korma Entity 'usage-records' defined")))

(defn -init
  []
  @init-db)

(defn- project   
  [usage-event metric]
  (-> usage-event
      (dissoc :metrics)
      (assoc  :metric_name   (:name  metric))
      (assoc  :metric_value  (:value metric))))

(defn- existing?
  [usage-event]
  (not
    (empty? 
      (kc/select usage-records 
        (kc/where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-event)})
        (kc/limit 1)))))

(defn- check-already-started
  [usage-event]
  (u/check existing? usage-event (str "Usage record not started: " usage-event)))  

(defn- extract-metrics
  [usage-event]
  (let [metrics (:metrics usage-event)]
    (if (zero? (count metrics))
      (log/warn "No metrics in " usage-event)      
      metrics)))

(defn- insert-metrics   
  [usage-event]
  (if (existing? usage-event)
    (log/warn (str "Usage record already inserted: " usage-event))    
    (doseq [metric (extract-metrics usage-event)]    
      (let [usage-event-metric (project usage-event metric)]
        (log/info "Will persist metric: " usage-event-metric)
        (kc/insert usage-records (kc/values usage-event-metric))))))

(defn- close-usage-record   
  [usage-event]
  (kc/update 
    usage-records 
    (kc/set-fields {:end_timestamp (:end_timestamp usage-event)})
    (kc/where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-event)})))

(defn -insertStart
  [usage-event]
  (log/info "Will persist usage event START:" usage-event)
  (-> usage-event
    u/walk-clojurify        
    insert-metrics))

(defn -insertEnd
  [usage-event]
  (log/info "Will persist usage event END:" usage-event) 
  (-> usage-event
    u/walk-clojurify
    check-already-started
    close-usage-record))

(defn insert-summary!   
  [summary]
  (let [summary-resource
         (-> summary
             (update-in   [:usage] u/serialize)
             (assoc :id   (cu/random-uuid))
             (assoc :acl  (u/serialize                               
                                {:owner  {:type "USER" :principal (:user summary)}
                                 :rules [{:type "USER" :principal (:user summary) :right "ALL"}]})))]    
    (kc/insert usage-summaries (kc/values summary-resource))))

(defn records-for-interval
  [start end]
  (u/check-order [start end])
  (kc/select usage-records (kc/where 
    (and 
      (or (= nil :end_timestamp) (>= :end_timestamp start))            
      (<= :start_timestamp end)))))
