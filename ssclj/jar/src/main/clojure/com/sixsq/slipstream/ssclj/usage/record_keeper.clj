(ns com.sixsq.slipstream.ssclj.usage.record-keeper
  (:require
    [superstring.core :refer [join]]
    [clojure.tools.logging :as log]
    [korma.core :as kc]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.usage.state-machine :as sm]
    [com.sixsq.slipstream.ssclj.api.acl :as acl]
    [com.sixsq.slipstream.ssclj.resources.usage-record :as ur]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.database.korma-helper :as kh]
    [com.sixsq.slipstream.ssclj.database.ddl :as ddl]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.db.impl :as db]))

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

    (dbdb/init-db)
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
      (assoc :metric-name (:name metric))
      (assoc :metric-value (:value metric))))

(defn- nil-timestamps-if-absent
  [usage-event]
  (merge {:end-timestamp nil :start-timestamp nil} usage-event))

(defn- usage-metrics
  [usage-event-json]
  (let [usage-event (-> usage-event-json
                        cu/walk-clojurify
                        nil-timestamps-if-absent)]
    (for [metric (:metrics usage-event)]
      (project-to-metric usage-event metric))))

(defn- state
  [usage-metric]
  (let [record (ur/last-record usage-metric)]
    (cond
      (nil? record) :initial
      (nil? (:end-timestamp record)) :started
      :else :stopped)))

(defn- open-instance-type?
  [metric]
  (and (.startsWith (:metric-name metric) "instance-type.")
       (nil? (:end-timestamp metric))))

;;
;; actions
;;
(defn- log-wrong-transition
  [state action]
  (log/info "Action" action " is not allowed for state " state))

(defn- metric-summary
  [usage-metric]
  (str (:start-timestamp usage-metric)
       " " (:cloud-vm-instanceid usage-metric)
       " " (:metric-name usage-metric)))

(defn- close-record
  ([usage-metric close-timestamp]
   (log/info "Close " close-timestamp (metric-summary usage-metric))
   (doseq [ur (ur/open-records usage-metric)]
     (db/edit (assoc ur :end-timestamp close-timestamp))))

  ([usage-metric]
   (close-record usage-metric (:end-timestamp usage-metric))))

(defn- close-metric-when-instance-type-change
  [usage-metric]
  (when (open-instance-type? usage-metric)
    (let [metrics-same-vm
          (kc/select usage_records (kc/where {:cloud-vm-instanceid (:cloud-vm-instanceid usage-metric)}))]
      (doseq [metric metrics-same-vm :when (open-instance-type? metric)]
        (close-record metric (:start-timestamp usage-metric))))))

(defn- open-record
  [usage-metric]
  (log/info "Open " (metric-summary usage-metric))
  ;; (close-metric-when-instance-type-change usage-metric)
  (db/add "UsageRecord" (assoc usage-metric :id (str "usage-record/" (cu/random-uuid)))))

(defn- close-restart-record
  [usage-metric]
  (close-record usage-metric (:start-timestamp usage-metric))
  (open-record usage-metric))

;;
;;
;;

(defn- process-event
  [usage-metric trigger]
  (let [current-state (state usage-metric)]
    (case (sm/action current-state trigger)
      :close-restart    (close-restart-record usage-metric)
      :insert-start     (open-record usage-metric)
      :wrong-transition (log-wrong-transition current-state trigger)
      :close-record     (close-record usage-metric))))

(defn- insertStart
  [usage-event-json]
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-event usage-metric :start)))

(defn- insertEnd
  [usage-event-json]
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-event usage-metric :stop)))

(defn insert-usage-event
  [usage-event]
  (if (nil? (:end-timestamp usage-event))
    (insertStart usage-event)
    (insertEnd usage-event)))

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

(defn- resource-for
  [summary acl]
  (-> summary
      (update-in [:usage] u/serialize)
      (assoc :id (str "usage/" (cu/random-uuid)))
      (assoc :acl (u/serialize acl))))

(defn- clause-where
  [row cols]
  (zipmap cols (map #(% row) cols)))

(defn insert-summary!
  [summary]
  (let [acl                   (acl-for-user-cloud summary)
        summary-resource      (resource-for summary acl)
        previous-computations (kc/select usage_summaries
                                         (kc/where
                                           (clause-where
                                             summary-resource [:user :cloud :start-timestamp :end-timestamp])))
        _
                              (doseq [previous-computation previous-computations]
                                (kc/delete usage_summaries (kc/where {:id (:id previous-computation)}))
                                (acl/-deleteResource (:id previous-computation) "Usage" (id-map-for-summary previous-computation)))]

    (kc/insert usage_summaries (kc/values (merge summary-resource {:compute-timestamp (u/now-to-ISO-8601)})))
    (acl/insert-resource (:id summary-resource) "Usage" (acl/types-principals-from-acl acl))))

(defn records-for-interval
  [start end]
  (ur/records-for-interval start end))

