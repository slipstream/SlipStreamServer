(ns slipstream.credcache.server
  "Utilities to start and stop the credential cache subsystem."
  (:require
    [clojure.tools.logging :as log]
    [clojurewerkz.quartzite.scheduler :as qs]
    [slipstream.credcache.db-utils :as db]))

(defn- start-quartz
  "Start up the queues for running jobs through the Quartz scheduler."
  []
  (qs/initialize)
  (qs/start))

(defn- stop-quartz
  "Stop the queues for running jobs through the Quartz scheduler.
   It will wait for running jobs to complete."
  []
  (qs/shutdown true))

(defn start!
  "Starts the credential management subsystem, creating the
   connection to the database and starting job queues."
  []
  (log/info "starting credential cache subsystem")
  (db/create-client)
  (start-quartz))

(defn stop!
  "Stops the credential management subsystem, freeing the
   resources for the Couchbase client and stopping the job
   queues."
  []
  (log/info "stopping credential cache subsystem")
  (stop-quartz)
  (db/destroy-client))
