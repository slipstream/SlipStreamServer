(ns com.sixsq.slipstream.ssclj.middleware.base-uri
  "middleware to add the :base-uri key and value to the request"
  (:require
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(defn get-host-port
  "Gets the originating host and port, preferring the 'forwarded'
   headers, if they exist.  The 'x-forwarded-for' is used instead
   of 'x-forwarded-host'."
  [{:keys [headers server-name server-port]}]
  (if-let [host (get headers "x-forwarded-for")]
    (if-let [port (get headers "x-forwarded-port")]
      (format "%s:%s" host port)
      host)
    (format "%s:%s" server-name server-port)))

(defn get-scheme
  "Get the scheme for the originating host, preferring the 'forwarded'
   header, it it exists."
  [{:keys [headers scheme]}]
  (or (get headers "x-forwarded-proto")
      (name scheme)))

(defn construct-base-uri
  [req]
  (format "%s://%s%s" (get-scheme req) (get-host-port req) p/service-context))

(defn wrap-base-uri
  "adds the :base-uri key to the request with the base URI value"
  [handler]
  (fn [req]
    (let [base-uri (construct-base-uri req)]
      (-> req
          (assoc :base-uri base-uri)
          (handler)))))
