(ns com.sixsq.slipstream.auth.app.middleware.wrap-credentials)

(defn wrap-credentials
  [handler]
  (fn [req]
    (-> req
        (assoc :user-name (get-in req [:params :user-name]))
        (assoc :password  (get-in req [:params :password]))
        handler)))