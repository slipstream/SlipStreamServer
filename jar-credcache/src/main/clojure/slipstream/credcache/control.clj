(ns slipstream.credcache.control
  "Utilities to start and stop the credential cache subsystem."
  (:require
    [clojure.tools.logging :as log]
    [clojurewerkz.quartzite.scheduler :as qs]
    [slipstream.credcache.db-utils :as db]
    [slipstream.credcache.credential :as cred]
    [slipstream.credcache.notify :as notify]
    [slipstream.credcache.credential.myproxy-voms]))

(defn- start-quartz
  "Start up the queues for running jobs through the Quartz scheduler."
  []
  (try
    (qs/initialize)
    (qs/start)
    (catch Exception e
      (log/error "error starting quartz scheduler:" (str e)))))

(defn- stop-quartz
  "Stop the queues for running jobs through the Quartz scheduler.
   It will wait for running jobs to complete."
  []
  (try
    (qs/shutdown true)
    (catch Exception e
      (log/error "error stopping quartz scheduler:" (str e)))))

(defn start!
  "Starts the credential management subsystem, starting job queues."
  []
  (log/info "starting credential cache subsystem")
  (db/create-cache-dir)
  (start-quartz)
  (cred/schedule-cleanup)
  (cred/reschedule-all-renewals))

(defn stop!
  "Stops the credential management subsystem, stopping the job
   queues."
  []
  (log/info "stopping credential cache subsystem")
  (stop-quartz))
