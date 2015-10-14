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
   :start_timestamp (days-from-now day-number)
   :end_timestamp   (days-from-now (inc day-number))
   :usage           (->> metrics-map
                         (map (fn [[k v]] {k {:unit_minutes v}}))
                         (into {}))})

(defn usages
  [nb username clouds]
  (for [day-number (range nb) cloud clouds]
    (daily-usage (name username) cloud day-number {:ram 47185920 :disk 450.67 :cpu 1250})))

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