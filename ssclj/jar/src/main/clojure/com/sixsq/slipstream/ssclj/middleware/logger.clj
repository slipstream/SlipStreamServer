(ns com.sixsq.slipstream.ssclj.middleware.logger
  (:require [clojure.tools.logging :as log]))

(defn- display-querystring
  [request]
  (let [to-display (-> request :query-string (or "no-query-string"))]
    (str "?" to-display)))

(defn display-authn-info
  [request]
  (let [to-display (-> request :headers (get "slipstream-authn-info")
                       (or "no-authn-info"))]
    (str "[" to-display "]")))

(defn log-request
  [request]
  (log/info (-> request :request-method)
            (-> request :uri)
            (-> request display-authn-info)
            (-> request display-querystring)
            (-> request :body (or "no-body")))
  request)

(defn wrap-logger
  "Logs the request. e.g:
   2015-09-11 13:37:04,619 INFO  - :get /api/usage [bob ADMIN] ?$first=1&$last=20 no-body
  "
  [handler]
  (fn [req]
    (-> req
        log-request
        handler)))
