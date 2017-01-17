(ns com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.test :refer [is]]
    [clojure.pprint :refer [pprint]]
    [compojure.core :as cc]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer [wrap-logger]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [wrap-authn-info-header]]))

(defn body->json
  [m]
  (if-let [body (get-in m [:response :body])]
    (let [updated-body (if (string? body)
                         (json/read-str body :key-fn keyword :eof-error? false :eof-value {})
                         (json/read (io/reader body) :key-fn keyword :eof-error? false :eof-value {}))]
      (update-in m [:response :body] (constantly updated-body)))
    m))

(defn is-status [m status]
  (is (= status (get-in m [:response :status])))
  m)

(defn is-key-value [m k v]
  (let [actual (get-in m [:response :body k])]
    (when-not (= v actual)
      (println "Expecting " v " got " actual))
    (is (= v actual))
    m))

(defn has-key [m k]
  (-> m
      (get-in [:response :body])
      (contains? k)
      is))

(defn is-resource-uri [m type-uri]
  (is-key-value m :resourceURI type-uri))

(defn is-operation-present [m op]
  (let [operations (get-in m [:response :body :operations])
        op (some #(.endsWith % op) (map :rel operations))]
    (is op))
  m)

(defn is-operation-absent [m op]
  (let [operations (get-in m [:response :body :operations])
        op (some #(.endsWith % op) (map :rel operations))]
    (is (nil? op)))
  m)

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
      wrap-keyword-params
      wrap-nested-params
      wrap-params



      (wrap-base-uri)
      (wrap-authn-info-header)
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})
      (wrap-logger)))

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
