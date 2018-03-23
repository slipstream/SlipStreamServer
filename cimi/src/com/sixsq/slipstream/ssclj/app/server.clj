(ns com.sixsq.slipstream.ssclj.app.server
  (:require
    [clojure.tools.logging :as log]

    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.nested-params :refer [wrap-nested-params]]
    [ring.middleware.cookies :refer [wrap-cookies]]

    [metrics.core :refer [default-registry remove-all-metrics]]
    [metrics.ring.instrument :refer [instrument]]
    [metrics.ring.expose :refer [expose-metrics-as-json]]
    [metrics.jvm.core :refer [instrument-jvm]]

    [com.sixsq.slipstream.ssclj.middleware.logger :refer [wrap-logger]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.graphite :as graphite]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as resources]
    [com.sixsq.slipstream.ssclj.app.utils :as utils]
    [environ.core :as env]
    [clojure.string :as str]))


(defn get-binding-namespaces
  []
  (-> :slipstream-db-bindings env/env (str/split #":")))

(defn initialize-bindings
  []
  (let [binding-namespaces (get-binding-namespaces)]
    (doall (map (partial utils/call-fn-from-namespace "initialize") binding-namespaces))))

(defn finalize-bindings
  []
  (map (partial utils/call-fn-from-namespace "finalize") (get-binding-namespaces)))

(defn- create-ring-handler
  "Creates a ring handler that wraps all of the service routes
   in the necessary ring middleware to handle authentication,
   header treatment, and message formatting."
  []
  (log/info "creating ring handler")

  (compojure.core/routes)

  (-> (routes/get-main-routes)

      ;;handler/site
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

  (finalize-bindings)

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

  (initialize-bindings)

  (let [handler (create-ring-handler)]
    (try
      (resources/initialize)
      (catch Exception e
        (log/warn "error initializing resources:" (str e))))

    (graphite/start-graphite-reporter)

    [handler stop]))
