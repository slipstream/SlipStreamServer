(ns slipstream.async.metric-updator
  (:require [clojure.java.shell :as sh])
  (:require [slipstream.async.log :as log])
  (:require [clojure.core.async :as async :refer [go timeout thread chan <! >! <!!]])
  (:import [com.sixsq.slipstream.measurements Measurements]))

(defn- get-measurements
  [user]
  (let [begin (System/currentTimeMillis)
        measurements (Measurements.)
        xml-str (.populateAsString measurements user)]
    (log/log-info (str "update took: " (- (System/currentTimeMillis) begin)))
    xml-str))

(defn print-sh
  [exit out err]
  (if (not= 0 exit)
    (log/log-error "Error calling metric update python script"))
  (log/log-info out)
  (when (not-empty err)
    (log/log-error err)))

(defn- report-metrics
  [path]
  (let [{:keys [exit out err]}
        (sh/sh "python" "ss-report-metrics.py" path
               :dir "/opt/slipstream/server/bin")]
    (print-sh exit out err)
    exit))

(defn update
  [user]
  (let [xml-str (get-measurements user)
        tmp-file (java.io.File/createTempFile (str "measures-" (.getName user)) ".xml")]
    (try
      (do
        (with-open [w (clojure.java.io/writer tmp-file)]
          (.write w xml-str))
        (report-metrics (.getAbsolutePath tmp-file)))
      (catch Exception e
        (do
          (log/log-warn "caught exception executing update: " (.getMessage e))
          -1))
      (finally (.delete tmp-file)))))

