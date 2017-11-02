(ns com.sixsq.slipstream.ssclj.resources.test-utils
  (:require
    [clojure.test :refer [is]]
    [superstring.core :as s]
    [ring.util.codec :as rc]

    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]

    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(defn- urlencode-param
  [p]
  (->> (re-seq #"([^=]*)=(.*)" p)
       first
       next
       (map rc/url-encode)
       (s/join "=")))

(defn urlencode-params
  [query-string]
  (if (empty? query-string)
    query-string
    (let [params (subs query-string 1)]
      (->> (s/split params #"&")
           (map urlencode-param)
           (s/join "&")
           (str "?")))))

(defn make-ring-app [resource-routes]
  (-> resource-routes
      wrap-base-uri
      wrap-cimi-params
      wrap-params
      wrap-authn-info-header
      wrap-exceptions
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})))

(defn ring-app []
  (make-ring-app (t/concat-routes routes/final-routes)))

(defn exec-request
  ([uri query-string auth-name]
   (-> (session (ring-app))
       (content-type "application/json")
       (header authn-info-header auth-name)
       (request (str uri (urlencode-params query-string))
                :content-type "application/x-www-form-urlencoded")
       (t/body->edn)))

  ([uri query-string auth-name http-verb body]
   (-> (session (ring-app))
       (content-type "application/json")
       (header authn-info-header auth-name)
       (request (str uri (urlencode-params query-string))
                :body (json/write-str body)
                :request-method http-verb
                :content-type "application/json")
       (t/body->edn))))

(defn exec-post
  [uri auth-name body]
  (exec-request uri "" auth-name :post body))

(defn is-count
  ([uri expected-count query-string auth-name]
   (-> (exec-request uri query-string auth-name)
       (t/is-status 200)
       (t/is-key-value :count expected-count))))

(defn are-counts
  ([key-to-count base-uri auth-name expected-count query-string]
   (are-counts key-to-count base-uri auth-name expected-count expected-count query-string))
  ([key-to-count base-uri auth-name expected-count expected-paginated-count query-string]
   (-> (exec-request base-uri query-string auth-name)
       (t/is-status 200)
       (t/is-key-value :count expected-count)
       (t/is-key-value count key-to-count expected-paginated-count))))
