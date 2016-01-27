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

(defn display-request-response
  [request response current-time-millis]
  (display-space-separated
    (-> response :status)
    (-> request (display-elapsed-time-millis current-time-millis))
    (-> request :request-method name (.toUpperCase))
    (-> request :uri)
    (-> request display-authn-info)
    (-> request display-querystring)
    (-> request :body (or "no-body"))))

(defn log-request-response
  [request response]
  (log/info (display-request-response request response (System/currentTimeMillis))))

(defn wrap-logger
  "Logs elements from request and response. e.g:
   2015-09-11 13:37:04,619 INFO  - 200 (125 ms) :get /api/usage [bob ADMIN] ?$first=1&$last=20 no-body
  "
  [handler]
  (fn [request]
    (let [response (handler request)]
      (log-request-response request response)
      response)))
