(ns com.sixsq.slipstream.ssclj.middleware.base-uri
  "middleware to add the :base-uri key and value to the request"
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(defn get-host-port
  "Get the host:port value for the request, preferring the 'host'
   header value over local server name and port."
  [{:keys [headers server-name server-port]}]
  (or (get headers "host")
      (format "%s:%d" server-name server-port)))

(defn get-scheme
  "Get the scheme to use for the base URI, preferring the header
   set by the proxy for the remote scheme being used (usually https)."
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
