(ns com.sixsq.slipstream.ssclj.usage.seeds.usages
  (:require
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]))

(defn days-ago-at-hour
  ([n h]
   (str (time/minus (time/today-at h 0 0 0) (time/days n))))
  ([n]
   (days-ago-at-hour n 0)))

(defn- daily-usage
  [username cloud day-number metrics-map]
  {:user            username
   :cloud           cloud
   :frequency       "daily"
   :start_timestamp (days-ago-at-hour day-number)
   :end-timestamp   (days-ago-at-hour (inc day-number))
   :usage           (->> metrics-map
                         (map (fn [[k v]] {k {:unit-minutes v}}))
                         (into {}))})

(defn- daily-records
  [username cloud day-number metrics-map]
  (for [[k v] metrics-map]
    {:user            username
     :cloud           cloud
     :start_timestamp (days-ago-at-hour day-number)
     :end-timestamp   (days-ago-at-hour day-number 10)
     :metric-name     k
     :metric_value    v}))

(defmulti usages-for-freq (comp first list))

(defmethod usages-for-freq :monthly
  [_ username cloud day-number]
  {:user            username
   :cloud           cloud
   :frequency       "monthly"
   :start_timestamp (days-ago-at-hour (* 30 day-number))
   :end-timestamp   (days-ago-at-hour (* 30 (inc day-number)))
   :usage           (->> {:ram 97185920 :disk 950.67 :cpu 9250}
                         (map (fn [[k v]] {k {:unit-minutes v}}))
                         (into {}))})

(defmethod usages-for-freq :weekly
  [_ username cloud day-number]
  {:user            username
   :cloud           cloud
   :frequency       "weekly"
   :start_timestamp (days-ago-at-hour (* 7 day-number))
   :end-timestamp   (days-ago-at-hour (* 7 (inc day-number)))
   :usage           (->> {:ram 571859200 :disk 5000.67 :cpu 52500}
                         (map (fn [[k v]] {k {:unit-minutes v}}))
                         (into {}))})

(defmethod usages-for-freq :daily
  [_ username cloud day-number]
  {:user            username
   :cloud           cloud
   :frequency       "daily"
   :start_timestamp (days-ago-at-hour day-number)
   :end-timestamp   (days-ago-at-hour (inc day-number))
   :usage           (->> {:ram 47185920 :disk 450.67 :cpu 1250}
                         (map (fn [[k v]] {k {:unit-minutes v}}))
                         (into {}))})

(defn usages
  [nb username clouds]
  (for [day-number (range nb) cloud clouds frequency [:daily :weekly :monthly]]
    (usages-for-freq frequency (name username) cloud day-number)))

(defn usage-records
  [nb username clouds]
  (flatten
    (for [day-number (range nb) cloud clouds]
      (daily-records (name username) cloud day-number {"ram" 4096 "disk" 120 "cpu" 2}))))

(defn insert-to-db
  [usages]
  (doseq [usage usages]
    (rc/insert-summary! usage {})))

(defn seed-summaries!
  [nb username clouds]
  (db/set-impl! (esb/get-instance))
  (-> nb
      (usages username clouds)
      insert-to-db))

(defn seed-records!
  [nb username clouds]
  (db/set-impl! (esb/get-instance))
  (let [records (usage-records nb username clouds)]
    (doseq [record records]
      (db/add "UsageRecord" (assoc record :id (str "usage-record/" (cu/random-uuid))) {}))))
