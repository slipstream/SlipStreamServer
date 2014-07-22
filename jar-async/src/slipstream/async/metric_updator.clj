(ns slipstream.async.metric-updator
  (:require [clojure.java.shell :as sh])
  (:require [slipstream.async.log :as log])
  (:require [clojure.core.async :as async :refer [go timeout thread chan <! >! <!!]])
  (:import [com.sixsq.slipstream.metering Metering])
  (:import [com.sixsq.slipstream.configuration Configuration]))

(defn metering-enabled?
  []
  (Configuration/getMeteringEnabled))

(defn update
  [user]
  (Metering/populate user))

