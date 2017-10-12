(ns com.sixsq.slipstream.ssclj.resources.job.utils
  (:require
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params.utils :as cimi-params-utils]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clj-time.core :as time]))

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