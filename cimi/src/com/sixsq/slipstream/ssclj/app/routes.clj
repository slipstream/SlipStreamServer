(ns com.sixsq.slipstream.ssclj.app.routes
  (:require
    [com.sixsq.slipstream.auth.auth :as auth]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.config :as cf]
    [com.sixsq.slipstream.util.response :as r]
    [compojure.core :refer [ANY defroutes DELETE GET let-routes POST PUT routes]]
    [compojure.route :as route]
    [ring.middleware.head :refer [wrap-head]]))

(def collection-routes
  (let-routes [uri (str p/service-context ":resource-name")]
    (POST uri request
      (crud/add request))
    (PUT uri request
      (crud/query request))
    (GET uri request
      (crud/query request))
    (ANY uri request
      (throw (r/ex-bad-method request)))))

(def resource-routes
  (let-routes [uri (str p/service-context ":resource-name/:uuid")]
    (GET uri request
      (crud/retrieve request))
    (PUT uri request
      (crud/edit request))
    (DELETE uri request
      (crud/delete request))
    (ANY uri request
      (throw (r/ex-bad-method request)))))

(def action-routes
  (let-routes [uri (str p/service-context ":resource-name/:uuid/:action")]
    (ANY uri request
      (crud/do-action request))))

(defn not-found
  "Route always returns a 404 error response as a JSON map."
  []
  (wrap-head
    (fn [{:keys [uri]}]
      (r/map-response "unknown resource" 404 uri))))

(def auth-routes
  (let-routes [uri-login (str p/auth-context "login")
               uri-logout (str p/auth-context "logout")]

    (POST uri-login request (auth/login request))
    (POST uri-logout request (auth/logout request))))

(def user-routes
  (let-routes [uri (str p/service-context ":resource-name{user}/:uuid{.*}")]
    (GET uri request
      (crud/retrieve request))
    (PUT uri request
      (crud/edit request))
    (DELETE uri request
      (crud/delete request))
    (ANY uri request
      (throw (r/ex-bad-method request)))))

(def final-routes
  [collection-routes
   user-routes
   resource-routes
   action-routes
   auth-routes
   (not-found)])

(defn get-main-routes
  "Returns all of the routes defined for the server.  This uses
   dynamic loading to discover all of the defined resources on the
   classpath."
  []
  (apply routes (doall (concat [(route/resources (str p/service-context "static"))]
                               (dyn/resource-routes)
                               final-routes))))
