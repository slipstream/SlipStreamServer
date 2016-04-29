(ns com.sixsq.slipstream.ssclj.usage.record-keeper
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.usage.state-machine :as sm]
    [com.sixsq.slipstream.ssclj.resources.usage-record :as ur]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
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
  ([usage-metric close-timestamp options]
   (log/info "Close " close-timestamp (metric-summary usage-metric))
   (doseq [ur (ur/open-records usage-metric)]
     (db/edit (assoc ur :end-timestamp close-timestamp) options)))
  ([usage-metric options]
   (close-record usage-metric (:end-timestamp usage-metric) options)))

(defn- open-record
  [usage-metric options]
  (log/info "Open " (metric-summary usage-metric))
  (db/add "UsageRecord"
          (assoc usage-metric :id (str "usage-record/" (cu/random-uuid)))
          options))

(defn- close-restart-record
  [usage-metric options]
  (close-record usage-metric (:start-timestamp usage-metric) options)
  (open-record usage-metric options))

(defn- process-metric-event
  [usage-metric trigger options]
  (let [current-state (state usage-metric)]
    (case (sm/action current-state trigger)
      :close-restart    (close-restart-record usage-metric options)
      :insert-start     (open-record usage-metric options)
      :wrong-transition (log-wrong-transition current-state trigger)
      :close-record     (close-record usage-metric options))))

(defn- insertStart
  [usage-event-json options]
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-metric-event usage-metric :start options)))

(defn- insertEnd
  [usage-event-json options]
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-metric-event usage-metric :stop options)))

(defn insert-usage-event
  [usage-event options]
  (if (nil? (:end-timestamp usage-event))
    (insertStart usage-event options)
    (insertEnd usage-event options)))

(defn- acl-for-user-cloud
  [summary]
  (let [user  (:user summary)
        cloud (:cloud summary)]

    {:owner {:type "USER" :principal user}
     :rules [{:type "USER" :principal user :right "ALL"}
             {:type "ROLE" :principal cloud :right "ALL"}]}))

(defn- resource-for
  [summary acl]
  (-> summary
      (update-in [:usage] u/serialize)
      (assoc :id (str "usage/" (cu/random-uuid)))
      (assoc :acl (u/serialize acl))))

(defn insert-summary!
  [summary options]
  (let [acl                   (acl-for-user-cloud summary)
        summary-resource      (resource-for summary acl)]
    (db/add "Usage"
            (merge summary-resource {:compute-timestamp (u/now-to-ISO-8601)})
            options)))

(defn records-for-interval
  [start end]
  (ur/records-for-interval start end))

