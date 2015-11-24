(ns com.sixsq.slipstream.ssclj.usage.seeds.usages
  (:require
    [korma.core :as kc]
    [com.sixsq.slipstream.ssclj.api.acl :as acl]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [clj-time.core :as time]))

(defn days-from-now
  [n]
  (str (time/plus (time/today-at-midnight) (time/days n))))

(defn- daily-usage
  [username cloud day-number metrics-map]
  {:user            username
   :cloud           cloud
   :frequency       "daily"
   :start_timestamp (days-from-now day-number)
   :end_timestamp   (days-from-now (inc day-number))
   :usage           (->> metrics-map
                         (map (fn [[k v]] {k {:unit_minutes v}}))
                         (into {}))})

(defn- daily-records
  [username cloud day-number metrics-map]
  (for [[k v] metrics-map]
    { :user            username
      :cloud           cloud
      :start_timestamp (days-from-now day-number)
      :end_timestamp   (days-from-now (inc day-number))
      :metric_name     k
      :metric_value    v}))

(defmulti usages-for-freq (comp first list))

(defmethod usages-for-freq :monthly
  [_ username cloud day-number]
  {:user            username
   :cloud           cloud
   :frequency       "monthly"
   :start_timestamp (days-from-now (* 30 day-number))
   :end_timestamp   (days-from-now (* 30 (inc day-number)))
   :usage           (->> {:ram 97185920 :disk 950.67 :cpu 9250}
                         (map (fn [[k v]] {k {:unit_minutes v}}))
                         (into {}))})

(defmethod usages-for-freq :weekly
  [_ username cloud day-number]
  {:user            username
   :cloud           cloud
   :frequency       "weekly"
   :start_timestamp (days-from-now (* 7 day-number))
   :end_timestamp   (days-from-now (* 7 (inc day-number)))
   :usage           (->> {:ram 571859200 :disk 5000.67 :cpu 52500}
                         (map (fn [[k v]] {k {:unit_minutes v}}))
                         (into {}))})

(defmethod usages-for-freq :daily
  [_ username cloud day-number]
  {:user            username
   :cloud           cloud
   :frequency       "daily"
   :start_timestamp (days-from-now day-number)
   :end_timestamp   (days-from-now (inc day-number))
   :usage           (->> {:ram 47185920 :disk 450.67 :cpu 1250}
                         (map (fn [[k v]] {k {:unit_minutes v}}))
                         (into {}))})

(defn usages
  [nb username clouds]
  (for [day-number (range nb) cloud clouds frequency [:daily :weekly :monthly]]
    (usages-for-freq frequency (name username) cloud day-number)))

(defn usage-records
  [nb username clouds]
  (flatten
    (for [day-number (range nb) cloud clouds]
      (daily-records (name username) cloud day-number {"ram" 2185520 "disk" 120.67 "cpu" 1250}))))

(defn insert-to-db
  [usages]
  (doseq [usage usages]
    (rc/insert-summary! usage)))

(defn seed-summaries!
  [nb username clouds & {:keys [clean]}]
  (db/set-impl! (dbdb/get-instance))
  (acl/-init)
  (rc/-init)
  (when clean
    (kc/delete rc/usage_summaries))
  (-> nb
      (usages username clouds)
      insert-to-db))

(defn seed-records!
  [nb username clouds & {:keys [clean]}]
  (db/set-impl! (dbdb/get-instance))
  (acl/-init)
  (rc/-init)
  (when clean
    (kc/delete rc/usage_records))
  (let [records (usage-records nb username clouds)]
    (doseq [record records]
      (kc/insert rc/usage_records (kc/values record)))))
