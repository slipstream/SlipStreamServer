(ns com.sixsq.slipstream.ssclj.usage.record-keeper
  (:require
    [clojure.tools.logging :as log]
    [superstring.core :as s]
    [com.sixsq.slipstream.ssclj.usage.state-machine :as sm]
    [com.sixsq.slipstream.ssclj.resources.usage-record :as ur]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.util.convert :as convert-utils]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.db.impl :as db]))

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

(defn end-in-future?
  [usage-event]
  (= ur/date-in-future (:end-timestamp usage-event)))

(defn- project-to-metric
  [usage-event metric]
  (-> usage-event
      (dissoc :metrics)
      (assoc :metric-name (:name metric))
      (assoc :metric-value (-> metric :value str))))

(defn- nil-timestamps-if-absent
  [usage-event]
  (merge {:end-timestamp nil :start-timestamp nil} usage-event))

(defn- usage-metrics
  [usage-event-json]
  (let [usage-event (-> usage-event-json
                        convert-utils/walk-clojurify
                        nil-timestamps-if-absent)]
    (for [metric (:metrics usage-event)]
      (project-to-metric usage-event metric))))

(defn- state
  [usage-metric]
  (let [record (ur/last-record usage-metric)]
    (cond
      (nil? record) :initial
      (end-in-future? record) :started
      :else :stopped)))

;;
;; actions
;;
(defn- log-wrong-transition
  [state action]
  (log/debug "Action" action " is not allowed for state " state))

(defn- metric-summary
  [usage-metric]
  (str (:start-timestamp usage-metric)
       "-" (:end-timestamp usage-metric)
       " " (:cloud-vm-instanceid usage-metric)
       " [" (:metric-name usage-metric) "/" (:metric-value usage-metric) "]"))

(defn- close-record
  ([usage-metric close-timestamp options]
   (log/debug "Closing usage-record " close-timestamp (metric-summary usage-metric))
   (log/debug "open urs " (ur/open-records usage-metric))
   (log/debug "close: options " options)
   (doseq [ur (ur/open-records usage-metric)]
     (log/debug "editing ur" ur)
     (db/edit (assoc ur :end-timestamp close-timestamp) options)))
  ([usage-metric options]
   (close-record usage-metric (:end-timestamp usage-metric) options)))

(defn- open-record
  [usage-metric options]
  (log/debug "Opening usage-record " (metric-summary usage-metric))
  (log/debug "open: options " options)
  (let [new-record (-> usage-metric
                       (assoc :resourceURI ur/resource-uri)
                       (assoc :id (str "usage-record/" (cu/random-uuid)))
                       (assoc :end-timestamp ur/date-in-future))]
    (db/add "UsageRecord" (ur/validate-fn new-record) options)))

(defn- close-restart-record
  [usage-metric options]
  (close-record usage-metric (:start-timestamp usage-metric) options)
  (open-record usage-metric options))

(defn- process-metric-event
  [usage-metric trigger options]
  (let [current-state (state usage-metric)]
    (case (sm/action current-state trigger)
      :close-restart (close-restart-record usage-metric options)
      :insert-start (open-record usage-metric options)
      :wrong-transition (log-wrong-transition current-state trigger)
      :close-record (close-record usage-metric options))))

(defn- insertStart
  [usage-event-json options]
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-metric-event usage-metric :start options)))

(defn- insertEnd
  [usage-event-json options]
  (log/debug "insertEnd usage-event-json " usage-event-json)
  (doseq [usage-metric (usage-metrics usage-event-json)]
    (process-metric-event usage-metric :stop options)))

(defn insert-usage-event
  [usage-event options]
  (if (nil? (:end-timestamp usage-event))
    (insertStart usage-event options)
    (insertEnd usage-event options)))

(defn- acl-for-user-cloud
  [summary]
  (let [user (:user summary)
        cloud (:cloud summary)]

    {:owner {:type "USER" :principal user}
     :rules [{:type "USER" :principal user :right "ALL"}
             {:type "ROLE" :principal cloud :right "ALL"}]}))

(defn- keys-dot->underscore
  [m]
  (->> m
       (into [])
       (map (fn [[k v]] [(-> k name (s/replace #"\." "_") keyword) v]))
       (into {})))

(defn- resource-for
  [summary acl]
  (-> summary
      (update-in [:usage] keys-dot->underscore)
      (assoc :id (str "usage/" (cu/random-uuid)))
      (assoc :acl acl)
      (assoc :compute-timestamp (u/now-to-ISO-8601))))

(defn insert-summary!
  [summary options]
  (let [acl (acl-for-user-cloud summary)
        summary-resource (resource-for summary acl)]
    (db/add "Usage" summary-resource options)))

(defn records-for-interval
  [start end]
  (ur/records-for-interval start end))

