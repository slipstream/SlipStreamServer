(ns com.sixsq.slipstream.ssclj.app.server
  (:require
    [clojure.tools.logging :as log]
    [compojure.handler :as handler]

    [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.cookies :refer [wrap-cookies]]

    [metrics.core :refer [default-registry]]
    [metrics.ring.instrument :refer [instrument]]
    [metrics.ring.expose :refer [expose-metrics-as-json]]
    [metrics.jvm.core :refer [instrument-jvm]]

    [com.sixsq.slipstream.ssclj.app.httpkit-container :as httpkit]
    [com.sixsq.slipstream.ssclj.app.aleph-container :as aleph]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer [wrap-logger]]
    [com.sixsq.slipstream.ssclj.middleware.proxy-redirect :refer [wrap-proxy-redirect]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params :refer [wrap-cimi-params]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as resources]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

;; FIXME: make this dynamic depending on the service configuration
(defn set-db-impl
  []
  ; (-> (fsdb/get-instance fsdb/default-db-prefix)
  ;     (db/set-impl!)))
  (db/set-impl! (dbdb/get-instance)))

(defn- create-ring-handler
  "Creates a ring handler that wraps all of the service routes
   in the necessary ring middleware to handle authentication,
   header treatment, and message formatting."
  []
  (log/info "creating ring handler")

  (instrument-jvm default-registry)

  (compojure.core/routes )

  (-> (routes/get-main-routes)

      handler/site
      wrap-exceptions
      wrap-cimi-params
      wrap-base-uri
      wrap-params
      wrap-authn-info-header

      (expose-metrics-as-json (str p/service-context "metrics") default-registry {:pretty-print? true})

      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})
      (instrument default-registry)

      wrap-cookies

      (wrap-proxy-redirect ["/api" "/auth"] "http://localhost:8080")

      wrap-logger)

  )

(defn start
  "Starts the server and returns a function that when called, will
   stop the application server."
  ([port]
    (start port "httpkit"))
  ([port impl]
   (log/info "=============== SSCLJ START" port "===============")
   (log/info "java vendor: " (System/getProperty "java.vendor"))
   (log/info "java version: " (System/getProperty "java.version"))
   (log/info "java classpath: " (System/getProperty "java.class.path"))
   (set-db-impl)
   (resources/initialize)
   (if (= impl "aleph")
     (-> (create-ring-handler)
         (aleph/start-container port))
     (-> (create-ring-handler)
         (httpkit/start-container port)))))

(defn stop
  "Stops the application server by calling the function that was
   created when the application server was started."
  [stop-fn]
  (try
    (and stop-fn (stop-fn))
    (log/info "shutdown application container")
    (catch Exception e
      (log/warn "application container shutdown failed:" (.getMessage e)))))
