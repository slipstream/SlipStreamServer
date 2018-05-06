(ns com.sixsq.slipstream.ssclj.app.server
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.ephemeral-impl :as edb]
    [com.sixsq.slipstream.ssclj.app.graphite :as graphite]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer [wrap-logger]]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as resources]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as zku]
    [environ.core :as env]
    [metrics.core :refer [default-registry remove-all-metrics]]
    [metrics.jvm.core :refer [instrument-jvm]]
    [metrics.ring.expose :refer [expose-metrics-as-json]]
    [metrics.ring.instrument :refer [instrument]]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.params :refer [wrap-params]]))


(def default-db-binding-ns "com.sixsq.slipstream.db.es.loader")


(defn load-db-binding
  [db-binding-ns]
  (try
    (require db-binding-ns)
    (catch Exception e
      (log/errorf "cannot require namespace %f: %s" db-binding-ns (.getMessage e))
      (throw e)))
  (try
    (let [load (-> db-binding-ns symbol find-ns (ns-resolve 'load))]
      (let [binding-impl (load)]
        (log/infof "created binding implementation from %s" db-binding-ns)
        binding-impl))
    (catch Exception e
      (log/errorf "error executing load function from %s: %s" db-binding-ns (.getMessage e))
      (throw e))))


(defn load-persistent-db-binding
  []
  (-> (env/env :persistent-db-binding-ns default-db-binding-ns)
      load-db-binding
      db/set-impl!))


(defn load-ephemeral-db-binding
  []
  (some-> (env/env :ephemeral-db-binding-ns)
          load-db-binding
          edb/set-impl!))


(defn- create-ring-handler
  "Creates a ring handler that wraps all of the service routes
   in the necessary ring middleware to handle authentication,
   header treatment, and message formatting."
  []
  (log/info "creating ring handler")

  (compojure.core/routes)

  (-> (routes/get-main-routes)

      ;; handler/site
      wrap-cimi-params
      wrap-base-uri
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      wrap-exceptions
      wrap-authn-info-header
      (expose-metrics-as-json (str p/service-context "metrics") default-registry {:pretty-print? true})
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})
      (instrument default-registry)
      wrap-logger
      wrap-cookies))


(defn stop
  "Stops the application server by calling the function that was
   created when the application server was started."
  []
  (graphite/stop-graphite-reporter)

  (try
    (zku/close-client!)
    (log/info "zookeeper client closed")
    (catch Exception e
      (log/warn "zookeeper client close failed:" (str e))))

  (db/close)

  (edb/close)

  (try
    (remove-all-metrics)
    (log/info "removed all instrumentation metrics")
    (catch Exception e
      (log/warn "failed removing all instrumentation metrics:" (str e)))))


(defn init
  []

  (try
    (instrument-jvm default-registry)
    (catch Exception e
      (log/warn "error registering instrumentation metrics:" (str e))))

  (load-persistent-db-binding)

  (load-ephemeral-db-binding)

  (try
    (zku/set-client! (zku/create-client))
    (catch Exception e
      (log/warn "error creating zookeeper client:" (str e))))

  (try
    (resources/initialize)
    (catch Exception e
      (log/warn "error initializing resources:" (str e))))

  (graphite/start-graphite-reporter)

  ;; returns tuple with handler and stop function
  [(create-ring-handler) stop])
