(ns com.sixsq.slipstream.ssclj.resources.test-utils
  (:require
    [clojure.test :refer [is]]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [clojure.string :as str]
    [ring.util.codec :as rc]
    [clj-time.core :as time]
    [clj-time.format :as time-fmt]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))


(defn to-time
  "Tries to parse the given string as a DateTime value.  Returns the DateTime
   instance on success and nil on failure."
  [s]
  (time-fmt/parse (:date-time time-fmt/formatters) s))


(defn- urlencode-param
  [p]
  (->> (re-seq #"([^=]*)=(.*)" p)
       first
       next
       (map rc/url-encode)
       (str/join "=")))


(defn urlencode-params
  [query-string]
  (if (empty? query-string)
    query-string
    (let [params (subs query-string 1)]
      (->> (str/split params #"&")
           (map urlencode-param)
           (str/join "&")
           (str "?")))))


(defn exec-request
  ([uri query-string auth-name]
   (-> (ltu/ring-app)
       session
       (content-type "application/json")
       (header authn-info-header auth-name)
       (request (str uri (urlencode-params query-string))
                :content-type "application/x-www-form-urlencoded")
       (ltu/body->edn)))

  ([uri query-string auth-name http-verb body]
   (-> (ltu/ring-app)
       session
       (content-type "application/json")
       (header authn-info-header auth-name)
       (request (str uri (urlencode-params query-string))
                :body (json/write-str body)
                :request-method http-verb
                :content-type "application/json")
       (ltu/body->edn))))


(defn is-count
  ([uri expected-count query-string auth-name]
   (-> (exec-request uri query-string auth-name)
       (ltu/is-status 200)
       (ltu/is-key-value :count expected-count))))


(defn are-counts
  ([key-to-count base-uri auth-name expected-count query-string]
   (are-counts key-to-count base-uri auth-name expected-count expected-count query-string))
  ([key-to-count base-uri auth-name expected-count expected-paginated-count query-string]
   (-> (exec-request base-uri query-string auth-name)
       (ltu/is-status 200)
       (ltu/is-key-value :count expected-count)
       (ltu/is-key-value count key-to-count expected-paginated-count))))


(def not-before? (complement time/before?))


(defn ordered-desc?
  [timestamps]
  (every? (fn [[a b]] (not-before? (to-time a) (to-time b))) (partition 2 1 timestamps)))


