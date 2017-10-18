(ns com.sixsq.slipstream.ssclj.resources.job.utils
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]))

(defn stop [{state :state id :id :as job}]
  (if (= state "RUNNING")
    (do
      (log/warn "Stopping job : " id)
      (assoc job :state "STOPPING"))
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
  (when-not (uzk/exists locking-queue-path)
    (uzk/create-all locking-queue-path :persistent? true)))
