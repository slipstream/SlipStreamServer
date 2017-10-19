(ns com.sixsq.slipstream.ssclj.resources.job.utils
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u])
  (:import (org.apache.commons.io.filefilter FalseFileFilter)))

(def state-running "RUNNING")
(def state-failed "FAILED")
(def state-stopping "STOPPING")
(def state-stopped "STOPPED")
(def state-success "SUCCESS")
(def state-queued "QUEUED")

(defn stop [{state :state id :id :as job}]
  (if (= state state-running)
    (do
      (log/warn "Stopping job : " id)
      (assoc job :state state-stopped))
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

(defn is-final-state? [{:keys [state] :as job}]
  (contains? #{state-failed state-success state-stopped} state))

(defn add-targetResource-in-affectedResources [{:keys [targetResource affectedResources] :as job}]
  (assoc job :affectedResources (conj affectedResources targetResource)))


(defn should_insert_targetResource-in-affectedResources? [{:keys [targetResource affectedResources] :as job}]
  (when targetResource
    (not-any? #(= targetResource %) affectedResources)))

(defn update-timeOfStatusChange [job]
  (assoc job :timeOfStatusChange (u/unparse-timestamp-datetime (time/now))))

(defn job-cond->addition [{:keys [progress statusMessage] :as job}]
  (cond-> job
          statusMessage update-timeOfStatusChange
          (not progress) (assoc :progress 0)
          (should_insert_targetResource-in-affectedResources? job) add-targetResource-in-affectedResources))

(defn job-cond->edition [job]
  (cond-> job
          (is-final-state? job) (assoc :progress 100)
          (should_insert_targetResource-in-affectedResources? job) add-targetResource-in-affectedResources))
