(ns com.sixsq.slipstream.ssclj.middleware.record-start)

(defn wrap-record-start
  "Inserts in request the current time in milliseconds."
  [handler]
  (fn [req]
    (-> req
        (assoc :logger-start (System/currentTimeMillis))
        handler)))
