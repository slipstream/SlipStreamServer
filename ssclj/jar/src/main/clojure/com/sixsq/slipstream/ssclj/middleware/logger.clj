(ns com.sixsq.slipstream.ssclj.middleware.logger
  (:require
    [superstring.core :as str]
    [clojure.tools.logging :as log]))

(defn- display-querystring
  [request]
  (str "?" (->  request
                :query-string
                (or "no-query-string")
                (str/replace #"&?password=([^&]*)" ""))))

(defn- display-authn-info
  [request]
  (let [to-display (-> request
                       :headers
                       (get "slipstream-authn-info")
                       (or "no-authn-info"))]
    (str "[" to-display "]")))

(defn- display-elapsed-time-millis
  [start current-time-millis]
  (str "(" (- current-time-millis start) " ms)"))

(defn- display-space-separated
  [& messages]
  (apply str (str/join " " messages)))

(defn display-request
  [request]
  (display-space-separated
    (-> request :request-method name (.toUpperCase))
    (-> request :uri)
    (-> request display-authn-info)
    (-> request display-querystring)
    (-> request :body (or "no-body"))))

(defn display-response
  [request response start current-time-millis]
  (display-space-separated
    (-> response :status)
    (display-elapsed-time-millis start current-time-millis)
    (-> request :request-method name (.toUpperCase))
    (-> request :uri)
    (-> request display-authn-info)
    (-> request display-querystring)
    (-> request :body (or "no-body"))))

(defn- log-level
  [request]
  (let [uri (:uri request)]
    (if (re-matches #".*(?:\.js|\.css|\.png|\.woff|\.woff2|\.svg)$" uri)
      :debug
      :info)))

(defn- log-response
  [request response start]
  (log/log
    (log-level request)
    (display-response request response start (System/currentTimeMillis))))

(defn- log-request
  [request]
  (log/log
    (log-level request)
    (display-request request)))

(defn wrap-logger
  "Logs both request and response e.g:
  2016-02-02 11:32:19,310 INFO  - GET /vms [no-authn-info] ?cloud=&offset=0&limit=20&moduleResourceUri=&activeOnly=1 no-body
  2016-02-02 11:32:19,510 INFO  - 200 (200 ms) GET /vms [no-authn-info] ?cloud=&offset=0&limit=20&moduleResourceUri=&activeOnly=1 no-body
  "
  [handler]
  (fn [request]
    (log-request request)
    (let [start       (System/currentTimeMillis)
          response    (handler request)]
      (log-response request response start)
      response)))
