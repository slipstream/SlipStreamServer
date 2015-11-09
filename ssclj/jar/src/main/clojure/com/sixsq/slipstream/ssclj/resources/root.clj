(ns com.sixsq.slipstream.ssclj.resources.root
  "Root collection for the server providing list of all resource collections."
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [compojure.core :refer [defroutes GET POST PUT DELETE ANY]]
    [ring.util.response :as r]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
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

;;
;; Root schema
;;

(def Root
  (merge c/CommonAttrs
         c/AclAttr
         {:baseURI  c/NonBlankString
          s/Keyword c/ResourceLink}))

;; dynamically loads all available resources
(def resource-links
  (into {} (dyn/get-resource-links)))

(def stripped-keys
  (concat (keys resource-links) [:baseURI :operations]))

;;
;; define validation function and add to standard multi-method
;;

(def validate-fn (u/create-validation-fn Root))
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
  "The Root resource is only created automatically at server startup
   if necessary.  It cannot be added through the API.  This function
   adds the minimal Root resource to the database."
  []
  (let [record (-> {:acl         resource-acl
                    :id          resource-url
                    :resourceURI resource-uri}
                   (u/update-timestamps))]
    (db/add resource-name record)))

(defn retrieve-impl
  [{:keys [base-uri] :as request}]
  (r/response (-> (db/retrieve resource-url)
                  (a/can-view? request)
                  (assoc :baseURI base-uri)
                  (merge resource-links)
                  (crud/set-operations request))))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(defn edit-impl
  [{:keys [body] :as request}]
  (let [current (-> (db/retrieve resource-url)
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
    (->> (apply dissoc updated stripped-keys)
         (db/edit))
    (r/response updated)))

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
;; Root doesn't follow the usual service-context + '/resource-name/UUID'
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
                (throw (u/ex-bad-method request))))
