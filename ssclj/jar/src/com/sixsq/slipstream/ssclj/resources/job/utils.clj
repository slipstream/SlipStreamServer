(ns com.sixsq.slipstream.ssclj.resources.job.utils
  (:require
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params.utils :as cimi-params-utils]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]))

(defn stop [{state :state id :id :as job}]
  (if (= state "RUNNING")
    (do
      (log/warn "Stopping job : " id)
      (assoc job :state "STOPPING"))
    job))

(defn status-changed? [{status :statusMessage :as job}]
  (if status
    (assoc job :timeOfStatusChange (u/unparse-timestamp-datetime (time/now)))
    job))

(def kazoo-queue-prefix "entry-")
(def kazoo-queue-priority "100")
(def job-base-node "/job")
(def locking-queue "/entries")
(def locking-queue-path (str job-base-node locking-queue))

(defn add-job-to-queue [job-id]
  (uzk/create (str locking-queue-path "/" kazoo-queue-prefix kazoo-queue-priority "-")
              :data (uzk/string-to-byte job-id) :sequential? true :persistent? true))

(defn create-job-queue []
  (when (not (uzk/exists locking-queue-path))
    (uzk/create-all locking-queue-path :persistent? true)))
