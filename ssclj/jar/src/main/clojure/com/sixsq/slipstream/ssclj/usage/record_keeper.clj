(ns com.sixsq.slipstream.ssclj.usage.record-keeper
  (:require
    [superstring.core :refer [join]]
    [clojure.tools.logging :as log]
    [korma.core :as kc]
    [com.sixsq.slipstream.ssclj.usage.state-machine :as sm]
    [com.sixsq.slipstream.ssclj.api.acl :as acl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.database.korma-helper :as kh]
    [com.sixsq.slipstream.ssclj.database.ddl :as ddl]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du])
  (:gen-class
    :name com.sixsq.slipstream.usage.Record
    :methods [

              #^{:static true} [init [] void]

              #^{:static true} [insertStart [java.util.Map] void]
              #^{:static true} [insertEnd [java.util.Map] void]]))

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
           "cloud_vm_instanceid" "VARCHAR(100)"
           "user" "VARCHAR(100)"
           "cloud" "VARCHAR(100)"
           "start_timestamp" "VARCHAR(30)"
           "end_timestamp" "VARCHAR(30)"
           "metric_name" "VARCHAR(100)"
           "metric_value" "FLOAT"))

(defonce ^:private columns-summaries
         (ddl/columns
           "id" "VARCHAR(100)"
           "acl" "VARCHAR(1000)"
           "user" "VARCHAR(100)"
           "cloud" "VARCHAR(100)"
           "start_timestamp" "VARCHAR(30)"
           "end_timestamp" "VARCHAR(30)"
           "frequency" "VARCHAR(30)"
           "grouping" "VARCHAR(100)"
           "compute_timestamp" "VARCHAR(30)"
           "usage" "VARCHAR(10000)"))

(defonce ^:private unique-summaries
         (str ", UNIQUE (" (ddl/double-quote-list ["user" "cloud" "start_timestamp" "end_timestamp"]) ")"))

(def init-db
  (delay
    (kh/korma-init)

    (acl/-init)

    (ddl/create-table! "usage_records" columns-record)
    (ddl/create-table! "usage_summaries" columns-summaries unique-summaries)

    (ddl/create-index! "usage_records" "IDX_TIMESTAMPS" "start_timestamp", "end_timestamp")
    (ddl/create-index! "usage_summaries" "IDX_TIMESTAMPS" "id" "start_timestamp", "end_timestamp")

    (kc/defentity usage_records (kc/database kh/korma-api-db))
    (kc/defentity usage_summaries (kc/database kh/korma-api-db))
    (kc/select usage_records (kc/limit 1))

    (log/info "record-keeper: Korma Entities defined")))

(defn -init
  []
  @init-db)

(defn- project-to-metric
  [usage-event metric]
  (-> usage-event
      (dissoc :metrics)
      (assoc :metric_name (:name metric))
      (assoc :metric_value (:value metric))))

(defn- nil-timestamps-if-absent
  [usage-event]
  (merge {:end_timestamp nil :start_timestamp nil} usage-event))


(defn- usage-metrics
  [usage-event-json]
  (let [usage-event (-> usage-event-json
                        cu/walk-clojurify
                        nil-timestamps-if-absent)]
    (for [metric (:metrics usage-event)]
      (project-to-metric usage-event metric))))

(defn last-record
  [usage-metric]
  (first
    (kc/select usage_records
               (kc/where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-metric)
                          :metric_name         (:metric_name usage-metric)})
               (kc/order :start_timestamp :DESC)
               (kc/limit 1))))

(defn state
  [usage-metric]
  (let [record (last-record usage-metric)]
    (cond
      (nil? record) :initial
      (nil? (:end_timestamp record)) :started
      :else :stopped)))

;;
;; actions
;;
(defn- log-wrong-transition
  [state action]
  (log/info "Action" action " is not allowed for state " state))


(defn- close-usage-record
  ([usage-metric close-timestamp]
   (log/info "Will record STOP for metric " (:metric_name usage-metric)
             "with timestamp: " close-timestamp
             ", usage-metric: " usage-metric)
   (kc/update
     usage_records
     (kc/set-fields {:end_timestamp close-timestamp})
     (kc/where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-metric)
                :metric_name         (:metric_name usage-metric)
                :end_timestamp       nil})))

  ([usage-metric]
   (close-usage-record usage-metric (:end_timestamp usage-metric))))

(defn- open-instance-type?
  [metric]
  (and (.startsWith (:metric_name metric) "instance-type.")
       (nil? (:end_timestamp metric))))

(defn close-metric-when-instance-type-change
  [usage-metric]
  (when (open-instance-type? usage-metric)
    (let [metrics-same-vm
          (kc/select usage_records (kc/where {:cloud_vm_instanceid (:cloud_vm_instanceid usage-metric)}))]
      (doseq [metric metrics-same-vm :when (open-instance-type? metric)]
          (close-usage-record metric (:start_timestamp usage-metric))))))

(defn- insert-metric
  [usage-metric]
  (log/info "Will record START for metric " (:metric_name usage-metric)
            ", usage-metric :" usage-metric)
  (close-metric-when-instance-type-change usage-metric)
  (kc/insert usage_records (kc/values usage-metric))
  (log/info "Done persisting metric: " usage-metric))

(defn close-restart-record
  [usage-metric]
  (close-usage-record usage-metric (:start_timestamp usage-metric))
  (insert-metric usage-metric))

;;
;;
;;

(defn- process-event
  [usage-metric trigger]
  (let [current-state (state usage-metric)]
    (case (sm/action current-state trigger)
      :close-restart    (close-restart-record usage-metric)
      :insert-start     (insert-metric usage-metric)
      :wrong-transition (log-wrong-transition current-state trigger)
      :close-record     (close-usage-record usage-metric))))

(defn -insertStart
  [usage-event-json]
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-event usage-metric :start)))

(defn -insertEnd
  [usage-event-json]
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-event usage-metric :stop)))

(defn- acl-for-user-cloud
  [summary]
  (let [user  (:user summary)
        cloud (:cloud summary)]

    {:owner {:type "USER" :principal user}
     :rules [{:type "USER" :principal user :right "ALL"}
             {:type "ROLE" :principal cloud :right "ALL"}]}))

(defn- id-map-for-summary
  [summary]
  (let [user  (:user summary)
        cloud (:cloud summary)]
    {:identity user
     :roles    [cloud]}))

(defn resource-for
  [summary acl]
  (-> summary
      (update-in [:usage] u/serialize)
      (assoc :id (str "usage/" (cu/random-uuid)))
      (assoc :acl (u/serialize acl))))

(defn clause-where
  [row cols]
  (zipmap cols (map #(% row) cols)))

(defn insert-summary!
  [summary]
  (let [acl                   (acl-for-user-cloud summary)
        summary-resource      (resource-for summary acl)
        previous-computations (kc/select usage_summaries
                                         (kc/where
                                           (clause-where
                                             summary-resource [:user :cloud :start_timestamp :end_timestamp])))
        _
                              (doseq [previous-computation previous-computations]
                                (kc/delete usage_summaries (kc/where {:id (:id previous-computation)}))
                                (acl/-deleteResource (:id previous-computation) "Usage" (id-map-for-summary previous-computation)))]

    (kc/insert usage_summaries (kc/values (merge summary-resource {:compute_timestamp (u/now-to-ISO-8601)})))
    (acl/insert-resource (:id summary-resource) "Usage" (acl/types-principals-from-acl acl))))

(defn records-for-interval
  [start end]
  (u/check-order [start end])
  (kc/select usage_records (kc/where
                             (and
                               (or (= nil :end_timestamp) (>= :end_timestamp start))
                               (<= :start_timestamp end)))))
