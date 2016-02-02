(ns com.sixsq.slipstream.ssclj.middleware.record-start
  (:require [com.sixsq.slipstream.ssclj.middleware.logger :as ml]))

(defn wrap-record-start
  "Inserts in request the current time in milliseconds."
  [handler]
  (fn [req]
    (ml/log-request req)
    (-> req
        (assoc :logger-start (System/currentTimeMillis))
        handler)))
