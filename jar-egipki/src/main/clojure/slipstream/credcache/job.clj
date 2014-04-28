(ns slipstream.credcache.job
  "Functions for managing the jobs for credential renewal."
  (:require
    [clojure.tools.logging :as log]
    [clojurewerkz.quartzite.scheduler :as qs]
    [clojurewerkz.quartzite.conversion :as qc]
    [clojurewerkz.quartzite.triggers :as t]
    [clojurewerkz.quartzite.jobs :as j]
    [clojurewerkz.quartzite.schedule.simple :refer [schedule with-repeat-count with-interval-in-milliseconds]]
    [clojurewerkz.quartzite.jobs :refer [defjob]]
    [slipstream.credcache.dbutils :as db]))

(def renewal-factor 2/3)              ;; renew 2/3 of way through validity period
(def renewal-threshold (* 5 60 1000)) ;; 5 minutes

(defn attempt-renewal
  "Attempts to renew and update the given credential.  Returns nil if the
   credential could not be found.  Returns the expiry information for the
   new or current credential depending on whether the credential was
   actually renewed."
  [id]
  (log/info "renewing credential:" id)
  (if-let [cred-info (db/retrieve-resource id)]
    (do
      (if-let [updated-cred-info "" #_(renew cred-info)]       ;; FIXME: Need real call to renewal.
        (do
          (db/update-resource id updated-cred-info)
          (log/info "credential renewed:" id)
          (:expiry updated-cred-info))
        (do
          (log/warn "credential not renewed:" id)
          (:expiry cred-info))))
    (do
      (log/warn "credential information not found:" id)
      nil)))

(defn calculate-delta
  "Calculate the delta between now and the expiration time, then
   apply the renewal factor time and the threshold for the delta.
   Returns nil if the delta is less than threshold."
  [expiry]
  (if expiry
    (let [delta (->> (System/currentTimeMillis)
                     (- expiry)
                     (max 0)
                     (* renewal-factor)
                     (int))]
      (if (>= delta renewal-threshold)
        delta))))

(declare schedule-renewal)

(defjob renew-cred-job
        [ctx]
        (let [m (qc/from-job-data ctx)
              id (get m "id")]
          (log/info "start credential renewal:" id)
          (->> id
               (attempt-renewal)
               (schedule-renewal id))
          (log/info "finished credential renewal: " id)))

(defn schedule-renewal
  [id expiry]
  (if-let [delta (calculate-delta expiry)]
    (let [job (j/build
                (j/of-type renew-cred-job)
                (j/using-job-data {"id" id})
                (j/with-identity (j/key (str "renewal." id))))
          trigger (t/build
                    (t/with-identity (t/key (str "trigger." id)))
                    (t/start-now)
                    (t/with-schedule (schedule
                                       (with-repeat-count 0)
                                       (with-interval-in-milliseconds delta))))]
      (qs/schedule job trigger)
      (log/info ("scheduled next renewal:" id delta)))
    (log/info "renewal NOT scheduled:" id)))
