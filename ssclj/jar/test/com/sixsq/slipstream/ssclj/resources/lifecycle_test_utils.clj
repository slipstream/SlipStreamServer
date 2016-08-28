(ns com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.test :refer [is]]
    [clojure.pprint :refer [pprint]]
    [compojure.core :as cc]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer [wrap-logger]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [wrap-authn-info-header]]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]))

(defn body->json
  [m]

  (if-let [body (get-in m [:response :body])]
    (let [updated-body (if (string? body)
                         (json/read-str body :key-fn keyword :eof-error? false :eof-value {})
                         (json/read (io/reader body) :key-fn keyword :eof-error? false :eof-value {}))]
      (update-in m [:response :body] (constantly updated-body)))
    m))

(defmacro is-set-cookie
  [m]
  `((fn [m#]
      (let [token# (-> m#
                       (get-in [:response :cookies])
                       vals
                       first
                       (get-in [:value :token]))
            ok?# (and token#
                      (re-matches #".+" token#)
                      (not (re-matches #".*INVALID.*" token#)))]
        (is ok?# (str "!!!! Set-Cookie header did not set cookie: " (or token# "nil")))
        m#)) ~m))

(defmacro is-unset-cookie
  [m]
  `((fn [m#]
      (let [token# (-> m#
                       (get-in [:response :cookies])
                       vals
                       first
                       (get-in [:value :token]))
            ok?# (and token#
                      (re-matches #".*INVALID.*" token#))]
        (is ok?# (str "!!!! Set-Cookie header did not invalidate cookie: " (or token# "nil")))
        m#)) ~m))

(defn is-status
  [m status]
  (let [actual (get-in m [:response :status])
        result (= status (get-in m [:response :status]))]
    (when-not result
      (println "!!!! Expecting status " status " got " actual))
    (is result)
    m))

(defn is-key-value
  ([m f k v]
   (let [actual (-> m :response :body k f)]
     (when-not (= v actual)
       (println "???? Expecting " v " got " actual " for " k))
     (is (= v actual))
     m))
  ([m k v]
   (is-key-value m identity k v)))

(defn has-key [m k]
  (-> m
      (get-in [:response :body])
      (contains? k)
      is))

(defn is-resource-uri [m type-uri]
  (is-key-value m :resourceURI type-uri))

(defn is-operation-present [m expected-op]
  (let [operations (get-in m [:response :body :operations])
        op (some #(.endsWith % expected-op) (map :rel operations))]
    (when-not op (println "???? Missing " expected-op " in " (map :rel operations)))
    (is op))
  m)

(defn is-operation-absent [m absent-op]
  (let [operations (get-in m [:response :body :operations])
        op (some #(.endsWith % absent-op) (map :rel operations))]
    (when op (println "???? Present " absent-op " in " (map :rel operations)))
    (is (nil? op)))
  m)

(defn operations->map [m]
  (into {} (map (juxt :rel :href) (:operations m))))

(defn is-id [m id]
  (is-key-value m :id id))

(defn is-count [m f]
  (let [count (get-in m [:response :body :count])]
    (is (f count))
    m))

(defn does-body-contain [m v]
  (let [body (get-in m [:response :body])]
    (is (= (merge body v) body))))

(defn location [m]
  (let [uri (get-in m [:response :headers "Location"])]
    (is uri)
    uri))

(defn entries [m k]
  (get-in m [:response :body k]))

(defn concat-routes
  [rs]
  (apply cc/routes rs))

(defn make-ring-app [resource-routes]
  (db/set-impl! (esb/get-instance))
  (-> resource-routes
      (wrap-exceptions)
      (wrap-cimi-params)
      (wrap-base-uri)
      (wrap-authn-info-header)
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})
      (wrap-logger)))

(defn dump-es
  [type]
  (println "DUMP")
  (clojure.pprint/pprint
    (esu/dump esb/*client* esb/index-name type)))

(defmacro with-test-client
  [& body]
  `(binding [esb/*client* (esb/create-test-client)]
     (db/set-impl! (esb/get-instance))
     (esu/reset-index esb/*client* esb/index-name)
     ~@body))

(defn with-test-client-fixture
  [f]
  (with-test-client
    (f)))
