(ns com.sixsq.slipstream.ssclj.middleware.proxy-redirect
  (:import [org.joda.time DateTime Interval])
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [puppetlabs.http.client.async :as ppasync]
    [puppetlabs.http.client.sync :as ppsync]
    [puppetlabs.http.client.common :as ppcommon]
    [ring.util.codec :as codec]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.util.parsing :as rp]
    [clj-time.core :refer [in-seconds]]
    [clj-time.format :refer [formatters unparse with-locale]]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    ))

;; Inspired by : https://github.com/tailrecursion/ring-proxy

(defonce client
         (ppasync/create-client {:force-redirects               false
                                 :follow-redirects              false
                                 :connect-timeout-milliseconds  10000
                                 :socket-timeout-milliseconds   10000 }))

(defn- uri-starts-with?
  [uri prefix]
  (.startsWith uri prefix))
(defn- build-url
  [host path]
  (.toString (java.net.URL. (java.net.URL. host) path)))

(defn slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))

(defn- slurp-body
  [request]
  (if-let [len (when (:body request) (get-in request [:headers "content-length"]))]
    (do
      (log/info "will slurp " len " bytes")
      (-> request
        :body
        (slurp-binary (Integer/parseInt len))
        String.))))


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

(def ^{:private true
       :doc "Attributes defined by RFC6265 that apply to the Set-Cookie header."}
set-cookie-attrs
  {:domain "Domain", :max-age "Max-Age", :path "Path"
   :secure "Secure", :expires "Expires", :http-only "HttpOnly"})

(defn- valid-attr?
  "Is the attribute valid?"
  [[key value]]
  (and (contains? set-cookie-attrs key)
       (not (.contains (str value) ";"))
       (case key
         :max-age (or (instance? Interval value) (integer? value))
         :expires (or (instance? DateTime value) (string? value))
         true)))

(def ^:private rfc822-formatter
  (with-locale (formatters :rfc822) java.util.Locale/US))

(defn- write-attr-map
  "Write a map of cookie attributes to a string."
  [attrs]
  {:pre [(every? valid-attr? attrs)]}
  (for [[key value] attrs]
    (let [attr-name (name (set-cookie-attrs key))]
      (cond
        (instance? Interval value) (str ";" attr-name "=" (in-seconds value))
        (instance? DateTime value) (str ";" attr-name "=" (unparse rfc822-formatter value))
        (true? value)  (str ";" attr-name)
        (false? value) ""
        :else (str ";" attr-name "=" value)))))

(defn- write-cookies
  "Turn a map of cookies into a seq of strings for a Set-Cookie header."
  [cookies encoder]
  (for [[key value] cookies]
    (if (map? value)
      (apply str (write-value key (:value value) encoder)
             (write-attr-map (dissoc value :value)))
      (write-value key value encoder))))

(defn- redirect
  [host request-uri request]

  (let [redirected-url  (build-url host request-uri)

        request-fn (case (:request-method request)
                     :get     ppcommon/get
                     :head    ppcommon/head
                     :post    ppcommon/post
                     :delete  ppcommon/delete
                     :put     ppcommon/put)

        response (request-fn client redirected-url
                             {:query-params (or {} (:params request))
                                            :body         (slurp-body request)
                              :headers      (-> request
                                                :headers
                                                (dissoc "host" "content-length"))})
        merged-cookies (merge (:cookies request) (parse-cookies @response codec/form-encode))]

    (log/info "redirect, status = " (:status @response))
    (log/info "redirect, (:cookies request) = " (:cookies request))
    (log/info "redirect, response cookies = " (parse-cookies @response codec/form-encode))
    (log/info "redirect, NEW cookies = " (codec/url-decode (str/join "; " (write-cookies merged-cookies codec/form-encode))))

    (if (<= 300 (:status @response) 307)
      (do
        (log/info "redirection, new loc = " (-> @response :headers (get "location")))
        (redirect host (-> @response :headers (get "location"))
                  (-> request
                      (assoc :request-method :get)
                      (assoc :body nil)
                      ;(assoc :cookies merged-cookies)
                      (assoc-in [:headers "Cookie"]
                                (codec/url-decode (str/join "; " (write-cookies merged-cookies codec/form-encode))))
                      )))

      (-> @response))))

(defn wrap-proxy-redirect
  [handler except-uri host]
  (fn [request]
    (let [request-uri (:uri request)]
      (if (uri-starts-with? request-uri except-uri)
        (handler request)
        (redirect host request-uri request)))))