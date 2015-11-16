(ns com.sixsq.slipstream.ssclj.middleware.proxy-redirect
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [puppetlabs.http.client.async :as ppasync]
    [puppetlabs.http.client.sync :as ppsync]
    [puppetlabs.http.client.common :as ppcommon]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [clj-time.core :refer [in-seconds]]
    [clj-time.format :refer [formatters unparse with-locale]])
  (:import (java.io ByteArrayInputStream)))

;; Inspired by : https://github.com/tailrecursion/ring-proxy

(def ^:const location-header-path [:headers "location"])

(def
  client (delay
           (ppasync/create-client {:force-redirects              false
                                   :follow-redirects             false
                                   :connect-timeout-milliseconds 60000
                                   :socket-timeout-milliseconds  60000})))

(defn- uri-starts-with?
  [uri prefixes]
  (some #(.startsWith uri %) prefixes))

(defn- build-url
  [host path query-string]
  (str (.toString (java.net.URL. (java.net.URL. host) path))
       (if query-string
         (str "?" query-string)
         "")))

(defn slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))

(defn- slurp-body-binary
  [request]
  (if-let [len (when (:body request) (get-in request [:headers "content-length"]))]
    (-> request
        :body
        (slurp-binary (Integer/parseInt len))
        ByteArrayInputStream.)))

(defn- write-value
  "Write the main cookie value."
  [key value encoder]
  (encoder {key value}))

(defn- split-equals
  [key-val]
  (let [index-equals (.indexOf key-val "=")
        len (count key-val)]
    (if (> index-equals -1)
      [(.substring key-val 0 index-equals)
       (.substring key-val (inc index-equals) len)]
      [])))

(defn to-query-params
  [query-string]
  (if (nil? query-string)
    {}
    (let [kvs (str/split query-string #"&")]
      (into {} (remove empty? (map split-equals kvs))))))

(defn- base-url
  [request]
  (str "https://" (get-in request [:headers "host"])))

(defn- after-scheme
  [url]
  (.substring url (.indexOf url "//")))

(defn- same-after-scheme?
  [urla urlb]
  (= (after-scheme urla) (after-scheme urlb)))

(defn rewrite-location
[location old-host new-host]

(let [result
      (if (and old-host new-host)
        (if (and location (not (same-after-scheme? location old-host)))
          (str/replace location (after-scheme old-host) (after-scheme new-host))
          (str new-host "/dashboard"))
        (or location ""))]

  (log/debug "rewrite location " location)
  (log/debug "rewrite old host" old-host)
  (log/debug "rewrite new host" new-host)
  (log/debug "rewrite result" result)
  result))

(defn update-location-header
      [response host req-host]
      (if-let [location (get-in response location-header-path)]
              (update-in response location-header-path #(rewrite-location % host req-host))
              response))

;; FIXME: Persistent client seems to be either caching credentials or not thread-safe.
#_(defn- redirect
       [host request-uri request]

       (let [redirected-url  (build-url host request-uri (:query-string request))

             request-fn (case (:request-method request)
                              :get     ppcommon/get
                              :head    ppcommon/head
                              :post    ppcommon/post
                              :delete  ppcommon/delete
                              :put     ppcommon/put)

             response (request-fn @client redirected-url
                                  {:query-params (merge (to-query-params (:query-string request)) (:params request))
                                   :body         (slurp-body-binary request)
                                   :headers      (-> request
                                                     :headers
                                                     (dissoc "host" "content-length"))})]

            (log/debug "response, status     = " (:status @response))

            (update-location-header @response host (base-url request))))

(defn- redirect
       [host request-uri request]

       (let [redirected-url  (build-url host request-uri (:query-string request))

             request-fn (case (:request-method request)
                              :get     ppsync/get
                              :head    ppsync/head
                              :post    ppsync/post
                              :delete  ppsync/delete
                              :put     ppsync/put)

             response (request-fn redirected-url
                                  {:query-params (merge (to-query-params (:query-string request)) (:params request))
                                   :body         (slurp-body-binary request)
                                   :headers      (-> request
                                                     :headers
                                                     (dissoc "host" "content-length"))})]

            (log/debug "response, status     = " (:status response))

            (update-location-header response host (base-url request))))

(defn wrap-proxy-redirect
  [handler except-uris host]
  (fn [request]
    (let [request-uri (:uri request)]
      (if (uri-starts-with? request-uri except-uris)
        (handler request)
        (redirect host request-uri request)))))

