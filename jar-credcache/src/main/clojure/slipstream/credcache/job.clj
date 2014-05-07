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
    [clj-time.coerce :as tc]
    [slipstream.credcache.db-utils :as db]
    [slipstream.credcache.renewal :as r]
    [slipstream.credcache.notify :as notify]))

(def renewal-factor 2/3)                                    ;; renew 2/3 of way through validity period
(def renewal-threshold (* 5 60))                            ;; 5 minutes in seconds!

(defn attempt-renewal
  "Attempts to renew and update the given credential.  Returns nil if the
   credential could not be found.  Returns the original or updated resource,
   depending on whether the renewal was successful or not."
  [id]
  (log/info "renewing credential:" id)
  (if-let [resource (db/retrieve-resource id)]
    (do
      (if-let [updated-resource (r/renew resource)]
        (do
          (db/update-resource id updated-resource)
          (log/info "credential renewed:" id)
          updated-resource)
        (do
          (log/warn "credential not renewed:" id)
          (notify/renewal-failure resource)
          resource)))
    (do
      (log/warn "credential information not found:" id)
      nil)))

(defn renewal-datetime
  "Returns the next renewal time for the given expiry date, applying
   the renewal wait time and threshold.  NOTE: The expiry
   time is in seconds, not milliseconds!"
  [expiry]
  (if expiry
    (let [now (quot (System/currentTimeMillis) 1000)
          delta (->> (- expiry now)
                     (max 0)
                     (* renewal-factor)
                     (int))]
      (if (>= delta renewal-threshold)
        (-> delta
            (+ now)
            (* 1000)
            (tc/from-long))))))

(declare schedule-renewal)

(defjob renew-cred-job
        [ctx]
        (let [id (-> ctx
                     (qc/from-job-data)
                     (get "id"))]
          (log/info "start credential renewal:" id)
          (->> id
               (attempt-renewal)
               (schedule-renewal))
          (log/info "finished credential renewal: " id)))

(defn schedule-renewal
  "Takes a resource (credential) as input and schedules a renewal job
   for that resource.  If the resource has no :expiry entry, then no
   renewal job is scheduled.  This function returns the original resource
   in all cases."
  [{:keys [id expiry] :as resource}]
  (if-let [start (renewal-datetime expiry)]
    (let [job-key (j/key (str "renewal." id))
          trigger-key (t/key (str "trigger." id))]
      (qs/delete-job job-key)
      (let [job (j/build
                  (j/with-identity job-key)
                  (j/of-type renew-cred-job)
                  (j/using-job-data {"id" id}))
            trigger (t/build
                      (t/with-identity trigger-key)
                      (t/start-at start))]
        (qs/schedule job trigger)
        (log/info "scheduled next renewal:" id start)))
    (log/info "renewal NOT scheduled:" id))
  resource)
