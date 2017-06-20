(ns com.sixsq.slipstream.ssclj.usage.summary
  (:require
    [clojure.tools.logging :as log]
    [clj-time.core :as t]
    [superstring.core :as s]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

;;
;; Cuts (truncate start or end timestamp) and aggregates usage records inside an interval.
;; Then summaries them.
;; Usage records must intersect with interval.
;;

(defn- intersect?
  [start-time end-time]
  (fn [usage-record]
    (and
      (t/before? (u/to-time (:start-timestamp usage-record)) (u/to-time end-time))
      (or
        (nil? (:end-timestamp usage-record))
        (t/after? (u/to-time (:end-timestamp usage-record)) (u/to-time start-time))))))

(defn- filter-inside-interval
  [start-time end-time usage-records]
  (filter (intersect? start-time end-time) usage-records))

(defn- shift-start
  [start]
  (fn [record]
    (let [record-start-time (:start-timestamp record)]
      (assoc record :start-timestamp (u/max-time start record-start-time)))))

(defn- shift-end
  [end]
  (fn [record]
    (let [record-end-time (:end-timestamp record)]
      (assoc record :end-timestamp (u/min-time end record-end-time)))))

(defn truncate
  [start-time end-time records]
  (u/check u/start-before-end? [start-time end-time] (str "Invalid timeframe: " (u/disp-interval start-time end-time)))
  (->> records
       (filter-inside-interval start-time end-time)
       (map (shift-start start-time))
       (map (shift-end end-time))))

(defn- remove-users
  [except-users records]
  (remove #((set except-users) (:user %)) records))

(defn contribution
  [record]
  (let [value (-> record :metric-value str read-string)
        nb-minutes (-> (u/to-interval (:start-timestamp record) (:end-timestamp record))
                       t/in-seconds
                       (/ 60.0))]
    (* value nb-minutes)))

(defn- comsumption
  [record]
  {:unit-minutes (contribution record)})

(defn- sum-consumptions
  [cons1 cons2]
  (update-in cons1 [:unit-minutes] #(+ % (:unit-minutes cons2))))

(defn- merge-summary-record
  [summary record]
  (let [record-metric (:metric-name record)
        record-comsumption (comsumption record)]
    (if-let [consumption-to-increase (get-in summary [:usage record-metric])]
      (assoc-in summary [:usage record-metric] (sum-consumptions consumption-to-increase record-comsumption))
      (assoc-in summary [:usage record-metric] record-comsumption))))

(defn- empty-summary-for-record
  [record start end frequency grouping-cols]
  (-> record
      (select-keys grouping-cols)
      (assoc :grouping (s/join "," (map name grouping-cols)))
      (assoc :frequency (name frequency))
      (assoc :start-timestamp start)
      (assoc :end-timestamp end)
      (assoc :usage {})))

(defn- merge-usages
  [records start end frequency grouping-cols]
  (let [summary-first-record (empty-summary-for-record (first records) start end frequency grouping-cols)]
    (reduce merge-summary-record summary-first-record records)))

(defn- summarize-groups-of-records
  [start end frequency grouping-cols records-grouped]
  (for [records records-grouped]
    (merge-usages records start end frequency grouping-cols)))

(defn summarize-records
  [records start-time end-time frequency grouping-cols & [except-users]]
  (->> records
       (remove-users except-users)
       (truncate start-time end-time)
       (group-by (fn [record] (select-keys record grouping-cols)))
       vals
       (summarize-groups-of-records start-time end-time frequency grouping-cols)))

(defn summarize
  [start-time end-time frequency grouping-cols & [except-users]]
  (summarize-records (rc/records-for-interval start-time end-time) start-time end-time frequency grouping-cols except-users))

(defn summarize-and-store!
  [start-time end-time frequency grouping-cols & [except-users]]
  (let [summaries (summarize start-time end-time frequency grouping-cols except-users)]
    (log/info "Will persist" (count summaries) "summaries for "
              (u/disp-interval start-time end-time) "except" except-users ", on" grouping-cols)
    (doseq [summary summaries]
      (rc/insert-summary! summary {:user-roles ["ADMIN"]}))))


