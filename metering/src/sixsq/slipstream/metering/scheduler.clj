(ns sixsq.slipstream.metering.scheduler
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)))

(def ^:const immediately 0)
(def ^:private num-threads 1)
(def ^:private pool (atom nil))

(defn- thread-pool []
  (swap! pool (fn [p] (or p (ScheduledThreadPoolExecutor. num-threads)))))

(defn periodically
  "Schedules function f to run every 'delay' milliseconds after a
  delay of 'initial-delay'."
  [f period]
  (.scheduleAtFixedRate (thread-pool)
                        f
                        immediately period TimeUnit/MINUTES))

(defn shutdown
  "Terminates all periodic tasks."
  []
  (swap! pool (fn [p] (when p (.shutdown p)))))
