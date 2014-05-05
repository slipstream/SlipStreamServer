(ns slipstream.credcache.control
  "Utilities to start and stop the credential cache subsystem."
  (:require
    [clojure.tools.logging :as log]
    [clojurewerkz.quartzite.scheduler :as qs]
    [slipstream.credcache.db-utils :as db]
    [slipstream.credcache.credential :as cred]
    [slipstream.credcache.notify :as notify]
    [slipstream.credcache.credential.myproxy-voms]          ;; FIXME: use dynamic loading
    ))

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
  "Starts the credential management subsystem, creating the
   connection to the database and starting job queues."
  [cb-params smtp-params]
  (log/info "starting credential cache subsystem")
  (try
    (notify/set-smtp-parameters! smtp-params)
    (catch Exception e
      (log/error "error setting SMTP parameters:" (str e))))
  (try
    (db/create-client cb-params)
    (catch Exception e
      (log/error "error creating Couchbase client:" (str e))))
  (db/add-design-doc)
  (start-quartz)
  (cred/reschedule-all-renewals))

(defn stop!
  "Stops the credential management subsystem, freeing the
   resources for the Couchbase client and stopping the job
   queues."
  []
  (log/info "stopping credential cache subsystem")
  (stop-quartz)
  (try
    (db/destroy-client)
    (catch Exception e
      (log/error "error destroying Couchbase client:" (str e)))))
