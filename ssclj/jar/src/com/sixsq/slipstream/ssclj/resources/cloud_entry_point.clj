(ns com.sixsq.slipstream.ssclj.resources.cloud-entry-point
  "The CloudEntryPoint resource provides the root list of all resources
   on the server."
  (:require
    [clojure.tools.logging :as log]
    [clojure.spec.alpha :as s]
    [compojure.core :refer [defroutes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.cloud-entry-point] ;; ensure schema is loaded
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.util.response :as sr]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]))

;;
;; utilities
;;

(def ^:const resource-name "CloudEntryPoint")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ANON"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;; dynamically loads all available resources
(def resource-links
  (into {} (dyn/get-resource-links)))

(def stripped-keys
  (concat (keys resource-links) [:baseURI :operations]))

;;
;; define validation function and add to standard multi-method
;;

(def validate-fn (u/create-spec-validation-fn :cimi/cloud-entry-point))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(defmethod crud/set-operations resource-uri
  [resource request]
  (try
    (a/can-modify? resource request)
    (let [ops [{:rel (:edit c/action-uri) :href resource-url}]]
      (assoc resource :operations ops))
    (catch Exception e
      (dissoc resource :operations))))

;;
;; CRUD operations
;;

(defn add
  "The CloudEntryPoint resource is only created automatically at server startup
   if necessary.  It cannot be added through the API.  This function
   adds the minimal CloudEntryPoint resource to the database."
  []
  (let [record (u/update-timestamps
                 {:acl         resource-acl
                  :id          resource-url
                  :resourceURI resource-uri})]
    (db/add resource-name record {})))

(defn retrieve-impl
  [{:keys [base-uri] :as request}]
  (r/response (-> (db/retrieve resource-url {})
                  ;; (a/can-view? request)
                  (assoc :baseURI base-uri)
                  (merge resource-links)
                  (crud/set-operations request))))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(defn edit-impl
  [{:keys [body] :as request}]
  (let [current (-> (db/retrieve resource-url {})
                    (assoc :acl resource-acl)
                    (a/can-modify? request))
        updated (-> body
                    (assoc :baseURI "http://example.org")
                    (u/strip-service-attrs))
        updated (-> (merge current updated)
                    (u/update-timestamps)
                    (merge resource-links)
                    (crud/set-operations request)
                    (crud/validate))]

    (db/edit updated request)))

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

;;
;; initialization: create cloud entry point if necessary
;;
(defn initialize
  []
  (try
    (add)
    (log/info "Created" resource-name "resource")
    (catch Exception e
      (log/warn resource-name "resource not created; may already exist; message: " (str e)))))

;;
;; CloudEntryPoint doesn't follow the usual service-context + '/resource-name/UUID'
;; pattern, so the routes must be defined explicitly.
;;
(defroutes routes
           (GET (str p/service-context resource-url) request
             (crud/retrieve (assoc-in request [:params :resource-name]
                                      resource-url)))
           (PUT (str p/service-context resource-url) request
             (crud/edit (assoc-in request [:params :resource-name]
                                  resource-url)))
           (ANY (str p/service-context resource-url) request
             (throw (sr/ex-bad-method request))))
