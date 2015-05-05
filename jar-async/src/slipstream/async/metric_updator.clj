(ns slipstream.async.metric-updator  
  (:import [com.sixsq.slipstream.metering Metering])
  (:import [com.sixsq.slipstream.configuration Configuration]))

(defn metering-enabled?
  []
  (Configuration/getMeteringEnabled))

(defn update-metric
  [user]
  (Metering/populate user))

