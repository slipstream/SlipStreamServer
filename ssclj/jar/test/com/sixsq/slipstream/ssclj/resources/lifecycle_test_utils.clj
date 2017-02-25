(ns com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer [is]]
    [clojure.pprint :refer [pprint]]
    [compojure.core :as cc]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer [wrap-logger]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [wrap-authn-info-header]]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]))

(defmacro is-status
  [m status]
  `((fn [m# status#]
      (let [actual# (get-in m# [:response :status])]
        (is (= status# actual#) (str "Expecting status " status# " got " (or actual# "nil")))
        m#)) ~m ~status))

(defmacro is-key-value
  ([m f k v]
   `((fn [m# f# k# v#]
       (let [actual# (-> m# :response :body k# f#)]
         (is (= v# actual#) (str "Expecting " v# " got " (or actual# "nil") " for " k#))
         m#)) ~m ~f ~k ~v))
  ([m k v]
   `(is-key-value ~m identity ~k ~v)))

(defmacro has-key
  [m k]
  `((fn [m# k#]
      (is (get-in m# [:response :body k#]) (str "Map did not contain key " k#)))
    ~m ~k))

(defmacro is-resource-uri
  [m type-uri]
  `(is-key-value ~m :resourceURI ~type-uri))

(defn select-op [m op]
  (let [op-map (get-in m [:response :body :operations])
        defined-ops (map :rel op-map)]
    [(some #(.endsWith % op) defined-ops) defined-ops]))

(defmacro is-operation-present [m expected-op]
  `((fn [m# expected-op#]
      (let [[op# defined-ops#] (select-op m# expected-op#)]
        (is op# (str "Missing " expected-op# " in " defined-ops#))
        m#))
    ~m ~expected-op))

(defmacro is-operation-absent [m absent-op]
  `((fn [m# absent-op#]
      (let [[op# defined-ops#] (select-op m# absent-op#)]
        (is (nil? op#) (str "Unexpected op " absent-op# " in " defined-ops#)))
      m#)
    ~m ~absent-op))

(defmacro is-id
  [m id]
  `(is-key-value ~m :id ~id))

(defmacro is-count
  [m f]
  `((fn [m# f#]
      (let [count# (get-in m# [:response :body :count])]
        (if (fn? f#)
          (is (f# count#) "Function of count did not return truthy value")
          (is (= f# count#) (str "Count wrong, expecting " f# ", got " (or count# "nil"))))
        m#)) ~m ~f))

(defn does-body-contain
  [m v]
  `((fn [m# v#]
      (let [body# (get-in m# [:response :body])]
        (is (= (merge body# v#) body#))))
    ~m ~v))

(defmacro is-set-cookie
  [m]
  `((fn [m#]
      (let [cookies# (get-in m# [:response :cookies])
            n# (count cookies#)
            token# (-> (vals cookies#)
                       first
                       :value
                       :token)]
        (is (= 1 n#) "incorrect number of cookies")
        (is (not= "INVALID" token#) "expecting valid token but got INVALID")
        (is (not (str/blank? token#)) "got blank token")
        m#)) ~m))

(defmacro is-unset-cookie
  [m]
  `((fn [m#]
      (let [cookies# (get-in m# [:response :cookies])
            n# (count cookies#)
            token# (-> (vals cookies#)
                       first
                       :value
                       :token)]
        (is (= 1 n#) "incorrect number of cookies")
        (is (= "INVALID" token#) "expecting INVALID but got different value")
        (is (not (str/blank? token#)) "got blank token")
        m#)) ~m))

(defmacro is-location
  [m]
  `((fn [m#]
      (let [uri-header# (get-in m# [:response :headers "Location"])
            uri-body# (get-in m# [:response :body :resource-id])]
        (is uri-header# "Location header was not set")
        (is uri-body# "Location (resource-id) in body was not set")
        (is (= uri-header# uri-body#) (str "!!!! Mismatch in locations, header=" uri-header# ", body=" uri-body#))
        m#)) ~m))

(defn location [m]
  (let [uri (get-in m [:response :headers "Location"])]
    (is uri "Location header missing from response")
    uri))

(defn operations->map [m]
  (into {} (map (juxt :rel :href) (:operations m))))

(defn body->edn
  [m]
  (if-let [body (get-in m [:response :body])]
    (let [updated-body (if (string? body)
                         (json/read-str body :key-fn keyword :eof-error? false :eof-value {})
                         (json/read (io/reader body) :key-fn keyword :eof-error? false :eof-value {}))]
      (update-in m [:response :body] (constantly updated-body)))
    m))

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
      wrap-keyword-params
      wrap-nested-params
      wrap-params
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
