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
  [request current-time-millis]
  (when-let [logger-start (:logger-start request)]
    (str "(" (- current-time-millis logger-start) " ms)")))

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

(defn display-request-response
  [request response current-time-millis]
  (display-space-separated
    (-> response :status)
    (-> response (display-elapsed-time-millis current-time-millis))
    (-> request :request-method name (.toUpperCase))
    (-> request :uri)
    (-> request display-authn-info)
    (-> request display-querystring)
    (-> request :body (or "no-body"))))

(defn- loggable?
  [request]
  (let [uri (:uri request)]
    (not (re-matches #".*(?:\.js|\.css|\.png|\.woff|\.svg)$" uri))))

(defn log-request-response
  [request response]
  (when (loggable? request)
    (log/info (display-request-response request response (System/currentTimeMillis)))))

(defn log-request
  [request]
  (when (loggable? request)
    (log/info (display-request request))))

(defn wrap-logger
  "Logs both request and response e.g:
  2016-02-02 11:32:19,310 INFO  - GET /vms [no-authn-info] ?cloud=&offset=0&limit=20&moduleResourceUri=&activeOnly=1 no-body
  2016-02-02 11:32:19,510 INFO  - 200 (200 ms) GET /vms [no-authn-info] ?cloud=&offset=0&limit=20&moduleResourceUri=&activeOnly=1 no-body
  "
  [handler]
  (fn [request]
    (log-request request)
    (let [response
          (-> request
              (assoc :logger-start (System/currentTimeMillis))
              handler)]
      (log-request-response request response)
      response)))
