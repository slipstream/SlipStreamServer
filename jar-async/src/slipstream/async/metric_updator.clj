(ns slipstream.async.metric-updator
  (:require [clojure.java.shell :as sh])
  (:require [slipstream.async.log :as log])
  (:require [clojure.core.async :as async :refer [go timeout thread chan <! >! <!!]])
  (:import [com.sixsq.slipstream.measurements Measurements]))

(defn- get-measurements
  [user]
  (let [begin (System/currentTimeMillis)
        measurements (Measurements.)
        measures (.populateAsString measurements user)]
    (log/log-info (str "update took: " (- (System/currentTimeMillis) begin)))
    measures))

(defn print-sh
  [exit out err]
  (if (not= 0 exit)
    (log/log-error "Error calling metric update python script"))
  (log/log-info out)
  (when (not-empty err)
    (log/log-error err)))

(defn- persist
  [measures]
  (let [{:keys [exit out err]}
        (sh/sh "python" "ss-report-metrics.py" "-" :in measures
               :dir "/opt/slipstream/server/bin")]
    (print-sh exit out err)
    exit))

(defn update
  [user]
  (let [measures (get-measurements user)]
    (if (empty? measures)
      0
      (try
        (persist measures)
        (catch Exception e 
          (do
            (log/log-warn "caught exception executing update: " (.getMessage e))
            -1))))))

