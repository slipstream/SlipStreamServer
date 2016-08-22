(ns com.sixsq.slipstream.ssclj.app.routes
  (:require
    [ring.middleware.head :refer [wrap-head]]
    [compojure.core :refer [defroutes let-routes routes POST GET PUT DELETE ANY]]
    [compojure.route :as route]

    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.util.config :as cf]

    [com.sixsq.slipstream.auth.auth :as auth]
    [com.sixsq.slipstream.auth.github :as gh]
    [com.sixsq.slipstream.auth.cyclone :as cy]))

(def collection-routes
  (let-routes [uri (str p/service-context ":resource-name")]
    (POST uri request
      (crud/add request))
    (GET uri request
      (crud/query request))
    (ANY uri request
      (throw (u/ex-bad-method request)))))

(def resource-routes
  (let-routes [uri (str p/service-context ":resource-name/:uuid")]
    (GET uri request
      (crud/retrieve request))
    (PUT uri request
      (crud/edit request))
    (DELETE uri request
      (crud/delete request))
    (ANY uri request
      (throw (u/ex-bad-method request)))))

(def action-routes
  (let-routes [uri (str p/service-context ":resource-name/:uuid/:action")]
    (ANY uri request
      (crud/do-action request))))

(defn not-found
  "Route always returns a 404 error response as a JSON map."
  []
  (wrap-head
    (fn [{:keys [uri]}]
      (u/map-response "unknown resource" 404 uri))))

(def auth-routes
  (let-routes [uri-login   (str p/auth-context "login")
               uri-logout  (str p/auth-context "logout")

               uri-token   (str p/auth-context "token")
               uri-github  (str p/auth-context "callback-github")
               uri-cyclone (str p/auth-context "callback-cyclone")]

    (POST uri-login request (auth/login request))
    (POST uri-logout request (auth/logout request))

    (POST uri-token request (auth/build-token request))

    (GET uri-github request (gh/callback-github request (cf/property-value :main-server)))
    (GET uri-cyclone request (cy/callback-cyclone request (cf/property-value :main-server)))))

(def final-routes
  [
   collection-routes
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
