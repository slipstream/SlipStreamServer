(ns com.sixsq.slipstream.ssclj.app.graphite
  (:require
   [clojure.tools.logging :as log]
   [metrics.reporters.graphite :as graphite]
   [environ.core :refer [env]])
  (:import
   [java.util.concurrent TimeUnit]
   [com.codahale.metrics MetricFilter]))

(defn create-reporter
  [host]
  (graphite/reporter {:host host
                      :prefix "slipstream"
                      :rate-unit TimeUnit/SECONDS
                      :duration-unit TimeUnit/MILLISECONDS
                      :filter MetricFilter/ALL}))

;; TODO: add a stop function for clean shutdown

(defn start-graphite-reporter
  []
  (if-let [host (env :graphite-host)]
    (try 
      (-> host
          (create-reporter)
          (graphite/start 10))
      (log/info "graphite metrics reporter started for" host)
      (catch Exception e
        (log/error "graphite metrics reporter error:" (.getMessage e))))
    (log/info "graphite metrics reporter NOT started")))
