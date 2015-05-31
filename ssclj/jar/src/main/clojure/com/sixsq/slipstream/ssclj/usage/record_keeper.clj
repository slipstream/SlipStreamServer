(ns com.sixsq.slipstream.ssclj.usage.record-keeper
  (:require 
    [clojure.string                                     :only [join]]
    [clojure.tools.logging                              :as log]
    [clojure.java.jdbc                                  :refer :all :as jdbc]
    [korma.core                                         :as kc]
    [com.sixsq.slipstream.ssclj.usage.state-machine     :as sm]
    [com.sixsq.slipstream.ssclj.api.acl                 :as acl]
    [com.sixsq.slipstream.ssclj.resources.common.utils  :as cu]
    [com.sixsq.slipstream.ssclj.database.korma-helper   :as kh]    
    [com.sixsq.slipstream.ssclj.database.ddl            :as ddl]
    [com.sixsq.slipstream.ssclj.usage.utils             :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils  :as du])
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
;; The usage_records is projected onto its metric dimensions, that means that
;; an original event on 3 dimensions will end up being stored as 3 rows.
;;
;; The records-for-interval retrieves the usage_records for a given interval.
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
    (log/info "Korma init done")    
    
    (acl/-init)

    (ddl/create-table! "usage_records"    columns-record)
    (ddl/create-table! "usage_summaries"  columns-summaries)

    (ddl/create-index! "usage_records"   "IDX_TIMESTAMPS" "start_timestamp", "end_timestamp")
    (ddl/create-index! "usage_summaries" "IDX_TIMESTAMPS" "start_timestamp", "end_timestamp")

    (kc/defentity usage_records)
    (kc/defentity usage_summaries)
    (kc/select usage_records (kc/limit 1))

    (log/info "Korma Entities defined")))

(defn -init
  []
  @init-db)

(defn- project-to-metric   
  [usage-event metric]
  (-> usage-event
      (dissoc :metrics)
      (assoc  :metric_name   (:name  metric))
      (assoc  :metric_value  (:value metric))))

(defn- extract-metrics
  [usage-event]
  (let [metrics (:metrics usage-event)]
    (if (zero? (count metrics))
      (log/warn "No metrics in " usage-event)      
      metrics)))

(defn first-record  
  [usage-event]
  (-> (kc/select usage_records 
        (kc/where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-event)})
        (kc/limit 1))
      first))

(defn state
  [usage-event]
  (let [record (first-record usage-event)] 
    (cond
      (nil? record)                 :initial
      (nil? (:end_timestamp record))  :started
      :else                           :stopped)))

;;
;; actions
;;
(defn- log-severe-wrong-transition  
  [state action]
  (log/fatal "Action" action " is not allowed for state " state))

(defn- insert-metrics   
  [usage-event]    
  (log/info "Will persist usage event START:" usage-event)
  (doseq [metric (extract-metrics usage-event)]    
    (let [usage-event-metric (project-to-metric usage-event metric)]
      (log/info "Will persist metric: " usage-event-metric)
      (kc/insert usage_records (kc/values usage-event-metric))
      (log/info "Done persisting metric: " usage-event-metric))))

(defn- close-usage-record   
  ([usage-event close-timestamp]
    (log/info "Will close usage event with" close-timestamp) 
    (kc/update 
      usage_records 
      (kc/set-fields {:end_timestamp close-timestamp})
      (kc/where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-event)})))
  ([usage-event]
    (close-usage-record usage-event (:end_timestamp usage-event))))

(defn reset-end   
  [usage-event]  
  (close-usage-record usage-event nil))   

;;
;;
;;

(defn- process-event   
  [usage-event trigger]
  (let [current-state (state usage-event)]
    (case (sm/action current-state trigger)      
      :insert-start             (insert-metrics usage-event)
      :severe-wrong-transition  (log-severe-wrong-transition current-state trigger)
      :reset-end                (reset-end usage-event)      
      :close-record             (close-usage-record usage-event))))

(defn- nil-timestamps-if-absent
  [usage-event]
  (merge {:end_timestamp nil :start_timestamp nil} usage-event))

(defn -insertStart
  [usage-event]  
  (-> usage-event
      u/walk-clojurify
      nil-timestamps-if-absent
      (process-event :start)))

(defn -insertEnd
  [usage-event]  
  (-> usage-event
      u/walk-clojurify
      nil-timestamps-if-absent
      (process-event :stop)))

(defn- acl-for-user-cloud   
  [summary]
  (let [user  (:user summary)
        cloud (:cloud summary)]

    { :owner  {:type "USER" :principal user}
      :rules [{:type "USER" :principal user  :right "ALL"}
              {:type "ROLE" :principal cloud :right "ALL"}]}))

(defn resource-for   
  [summary acl]  
  (-> summary
      (update-in   [:usage] u/serialize)
      (assoc :id   (str "Usage/" (cu/random-uuid)))
      (assoc :acl  (u/serialize acl))))  

(defn insert-summary!   
  [summary]
  (let [acl                 (acl-for-user-cloud summary)
        summary-resource    (resource-for summary acl)]    
    (kc/insert usage_summaries (kc/values summary-resource))    
    (acl/insert-resource (:id summary-resource) "Usage" (acl/types-principals-from-acl acl))))

(defn records-for-interval
  [start end]
  (u/check-order [start end])
  (kc/select usage_records (kc/where 
    (and 
      (or (= nil :end_timestamp) (>= :end_timestamp start))            
      (<= :start_timestamp end)))))
