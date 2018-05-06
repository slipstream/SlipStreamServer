(ns com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.test :refer [is]]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.db.es.common.utils :as escu]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.dbtest.es.utils :as esut]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer [wrap-logger]]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [compojure.core :as cc]
    [peridot.core :refer [request session]]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.util.codec :as codec]
    [zookeeper :as zk]
    [qbits.spandex :as spandex])
  (:import [org.apache.curator.test TestingServer]))


(defn serialize-cookie-value
  "replaces the map cookie value with a serialized string"
  [{:keys [value] :as cookie}]
  (if value
    (assoc cookie :value (codec/form-encode value))
    cookie))


(defmacro message-matches
  [m re]
  `((fn [m# re#]
      (let [message# (get-in m# [:response :body :message])]
        (is (re-matches re# message#) (str "Message does not match pattern. " (or message# "nil") " " re#))
        m#)) ~m ~re))


(defmacro is-status
  [m status]
  `((fn [m# status#]
      (let [actual# (get-in m# [:response :status])]
        (is (= status# actual#) (str "Expecting status " status# " got " (or actual# "nil") ". Message: "
                                     (get-in m# [:response :body :message])))
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


(defn get-op [m op]
  (->> (get-in m [:response :body :operations])
       (map (juxt :rel :href))
       (filter (fn [[rel href]] (.endsWith rel op)))
       first
       second))


(defn select-op [m op]
  (let [op-list (get-in m [:response :body :operations])
        defined-ops (map :rel op-list)]
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
                       serialize-cookie-value
                       :value)]
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
                       serialize-cookie-value
                       :value)]
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


(defn dump
  [response]
  (pprint response)
  response)


(defn dump-m
  [response message]
  (println "-->>" message)
  (pprint response)
  (println message "<<--")
  response)


(defn dump-message
  [request]
  (println (get-in request [:response :body :message]))
  request)


(defn refresh-es-indices
  []
  (let [client (spandex/client {:hosts ["localhost:9200"]})]
    (spandex/request client {:url [:_refresh], :method :post})
    (spandex/close! client)))


(defn strip-unwanted-attrs
  "Strips common attributes that are not interesting when comparing
   versions of a resource."
  [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description :properties}]
    (into {} (remove #(unwanted (first %)) m))))


;;
;; Handling of Zookeeper server and client
;;

(defn create-zk-client-server
  []
  (let [port 21810]
    (log/info "creating zookeeper server on port" port)
    (let [server (TestingServer. port)
          client (zk/connect (str "127.0.0.1:" port))]
      (uzk/set-client! client)
      [client server])))


(def ^:private zk-client-server-cache (atom nil))


(defn set-zk-client-server-cache
  "Sets the value of the cached Elasticsearch node and client. If the current
   value is nil, then a new node and a new client are created and cached. If
   the value is not nil, then the cache is set to the same value. This returns
   the tuple with the node and client, which should never be nil."
  []
  ;; Implementation note: It is unfortunate that the atom will constantly be
  ;; reset to the current value because swap! is used.  Unfortunately,
  ;; compare-and-set! can't be used because we want to avoid unnecessary
  ;; creation of ring application instances.
  (swap! zk-client-server-cache (fn [current] (or current (create-zk-client-server)))))


(defn clear-zk-client-server-cache
  "Unconditionally clears the cached Elasticsearch node and client. Can be
   used to force the re-initialization of the node and client. If the current
   values are not nil, then the node and client will be closed, with errors
   silently ignored."
  []
  (let [[[client server] _] (swap-vals! zk-client-server-cache (constantly nil))]
    (when client
      (try
        (.close client)
        (catch Exception _)))
    (when server
      (try
        (.close server)
        (catch Exception _)))))


;;
;; Handling of Elasticsearch node and client for tests
;;

(defn create-es-node-client
  []
  (log/info "creating elasticsearch node and client")
  (let [node (esut/create-test-node)
        client (-> node
                   esu/node-client
                   esu/wait-for-cluster)]
    [node client]))


(def ^:private es-node-client-cache (atom nil))


(defn set-es-node-client-cache
  "Sets the value of the cached Elasticsearch node and client. If the current
   value is nil, then a new node and a new client are created and cached. If
   the value is not nil, then the cache is set to the same value. This returns
   the tuple with the node and client, which should never be nil."
  []
  ;; Implementation note: It is unfortunate that the atom will constantly be
  ;; reset to the current value because swap! is used.  Unfortunately,
  ;; compare-and-set! can't be used because we want to avoid unnecessary
  ;; creation of ring application instances.
  (swap! es-node-client-cache (fn [current] (or current (create-es-node-client)))))


(defn clear-es-node-client-cache
  "Unconditionally clears the cached Elasticsearch node and client. Can be
   used to force the re-initialization of the node and client. If the current
   values are not nil, then the node and client will be closed, with errors
   silently ignored."
  []
  (let [[[node client] _] (swap-vals! es-node-client-cache (constantly nil))]
    (when client
      (try
        (.close client)
        (catch Exception _)))
    (when node
      (try
        (.close node)
        (catch Exception _)))))


(defmacro with-test-es-client
  "Creates an Elasticsearch test client, executes the body with the created
   client bound to the Elasticsearch client binding, and then clean up the
   allocated resources by closing both the client and the node."
  [& body]
  `(let [client# (second (set-es-node-client-cache))]
     (db/set-impl! (esb/->ESBindingLocal client#))
     (esu/reset-index client# (str escu/default-index-prefix "*"))
     ~@body))


;;
;; Ring Application Management
;;

(defn make-ring-app [resource-routes]
  (log/info "creating ring application")
  (-> resource-routes
      wrap-cimi-params
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      wrap-base-uri
      wrap-authn-info-header
      wrap-exceptions
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})
      wrap-logger))


(def ^:private ring-app-cache (atom nil))

(defn set-ring-app-cache
  "Sets the value of the cached ring application. If the current value is nil,
   then a new ring application is created and cached. If the value is not nil,
   then the cache is set to the same value. This returns the ring application
   value, which should never be nil."
  []
  ;; Implementation note: It is unfortunate that the atom will constantly be
  ;; reset to the current value because swap! is used.  Unfortunately,
  ;; compare-and-set! can't be used because we want to avoid unnecessary
  ;; creation of ring application instances.
  (swap! ring-app-cache (fn [current] (or current
                                          (make-ring-app (concat-routes [(routes/get-main-routes)]))))))


(defn clear-ring-app-cache
  "Unconditionally clears the cached ring application instance.  Can be used
   to force the re-initialization of the ring application."
  []
  (reset! ring-app-cache (constantly nil)))


(defn ring-app
  "Returns a standard ring application with the CIMI server routes. By
   default, only a single instance will be created and cached. The cache can be
   cleared with the `clean-ring-app-cache` function."
  []
  (set-ring-app-cache))


;;
;; test fixture to ensure that all parts of the test server are started
;; (elasticsearch, zookeeper, ring application)
;;

(defn with-test-server-fixture
  "This fixture will ensure that Elasticsearch and zookeeper instances are
   running. It will also create a ring application and initialize it. The
   servers and application are cached to eliminate unnecessary instance
   creation."
  [f]
  (log/debug "executing with-test-server-fixture")
  (set-zk-client-server-cache)                              ;; always setup the zookeeper client and server
  (with-test-es-client
    (ring-app)
    (log/info "forced initialization of ring application")
    (dyn/initialize)                                        ;; must always reinitialize after database has been cleared
    (f)))


;;
;; miscellaneous utilities
;;

(defn verify-405-status
  "The url-methods parameter must be a list of URL/method tuples. It is
  expected that any request with the method to the URL will return a 405
  status."
  [url-methods]
  (doall
    (for [[uri method] url-methods]
      (-> (ring-app)
          session
          (request uri
                   :request-method method
                   :body (json/write-str {:dummy "value"}))
          (is-status 405)))))


