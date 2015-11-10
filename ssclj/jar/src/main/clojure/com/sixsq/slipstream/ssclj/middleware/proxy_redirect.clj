(ns com.sixsq.slipstream.ssclj.middleware.proxy-redirect
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [puppetlabs.http.client.async :as ppasync]
    [puppetlabs.http.client.common :as ppcommon]
    [ring.util.codec :as codec]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.util.parsing :as rp]
    [clj-time.core :refer [in-seconds]]
    [clj-time.format :refer [formatters unparse with-locale]]))

;; Inspired by : https://github.com/tailrecursion/ring-proxy

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
        (slurp-binary (Integer/parseInt len)))))

(defn- slurp-body
  [request]
  (if-let [len (when (:body request) (get-in request [:headers "content-length"]))]
    (-> request
        :body
        (slurp-binary (Integer/parseInt len))
        String.)))

(def ^{:private true, :doc "RFC6265 cookie-octet"}
re-cookie-octet
  #"[!#$%&'()*+\-./0-9:<=>?@A-Z\[\]\^_`a-z\{\|\}~]")

(def ^{:private true, :doc "RFC6265 cookie-value"}
re-cookie-value
  (re-pattern (str "\"" re-cookie-octet "*\"|" re-cookie-octet "*")))

(def ^{:private true, :doc "RFC6265 set-cookie-string"}
re-cookie
  (re-pattern (str "\\s*(" rp/re-token ")=(" re-cookie-value ")\\s*[;,]?")))

(defn- parse-cookie-header
  "Turn a HTTP Cookie header into a list of name/value pairs."
  [header]
  (for [[_ name value] (re-seq re-cookie header)]
    [name value]))

(defn- strip-quotes
  "Strip quotes from a cookie value."
  [value]
  (str/replace value #"^\"|\"$" ""))

(defn- decode-values [cookies decoder]
  (for [[name value] cookies]
    (if-let [value (decoder (strip-quotes value))]
      [name {:value value}])))

(defn urldecode-values
  [cookies]
  (for [[name map-value] cookies]
    [name {:value (codec/url-decode (:value map-value))}]))

(defn- parse-cookies
  "Parse the cookies from a request map."
  [request encoder]
  (if-let [cookie (get-in request [:headers "set-cookie"])]
    (->> cookie
         parse-cookie-header
         ((fn [c] (decode-values c encoder)))
         urldecode-values
         (remove nil?)
         (into {}))
    {}))

(defn- write-value
  "Write the main cookie value."
  [key value encoder]
  (encoder {key value}))

(defn- str-set-cookie
  [response]
  (let [cookie-in-response (parse-cookies response codec/form-encode)]
    (if-not (empty? cookie-in-response)
      (str (get (first cookie-in-response) 0) "=" (:value (get (first cookie-in-response) 1)))
      "")))

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

(defn- redirect
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
                              :body         (if-let [accept (get-in request [:request "accept-encoding"])]
                                              (if (.startsWith accept "gzip")
                                                (slurp-body-binary request)
                                                (slurp-body request))
                                              (slurp-body request))
                              :headers      (-> request
                                                :headers
                                                (dissoc "host" "content-length"))})]

    (log/debug "response, status     = " (:status @response))

    (-> @response
        (assoc-in  [:headers "set-cookie"] (str-set-cookie @response))
        (update-in [:headers "location"]  #(rewrite-location %
                                                             host
                                                             (base-url request))))))

(defn wrap-proxy-redirect
  [handler except-uris host]
  (fn [request]
    (let [request-uri (:uri request)]
      (if (uri-starts-with? request-uri except-uris)
        (handler request)
        (redirect host request-uri request)))))
