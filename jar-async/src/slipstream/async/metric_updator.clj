(ns slipstream.async.metric-updator
  (:require [clojure.java.shell :as sh])
  (:require [slipstream.async.log :as log])
  (:require [clojure.core.async :as async :refer [go timeout thread chan <! >! <!!]])
  (:import [com.sixsq.slipstream.measurements Measurements]))

(defn- get-measurements
  [user]
  (let [measurements (Measurements.)
        measures (.populateAsString measurements user)]
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
        (sh/sh 
          "python" "-m" "slipstream.metering.tasks" "--data" measures)]
    (print-sh exit out err)
    exit))

(defn update
  [user]
  (let [measures (get-measurements user)]
    (if (empty? measures)
      0
      (persist measures))))
      