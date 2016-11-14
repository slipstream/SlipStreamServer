(ns com.sixsq.slipstream.ssclj.resources.configuration
  (:require
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-tag :configurations)

(def ^:const resource-name "Configuration")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ConfigurationCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}]})
;;
;; schemas
;;

(def Configuration
  (merge c/CommonAttrs
         c/AclAttr
         {:service c/NonBlankString}))

(def ConfigurationCreate
  (merge c/CreateAttrs
         {:configurationTemplate tpl/ConfigurationTemplateRef}))

;;
;; validate subclasses of configurations
;;

(defmulti validate-subtype
          :service)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown Configuration type: " (:service resource)) resource)))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of configurations
;;

(defn dispatch-on-service [resource]
  (get-in resource [:configurationTemplate :service]))

(defmulti create-validate-subtype dispatch-on-service)

(defmethod create-validate-subtype :default
  [resource]
  (throw (ex-info (format "unknown Configuration create type: %s %s" (dispatch-on-service resource) resource) resource)))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))

;;
;; multimethod for ACLs
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; template processing
;;

(defmulti tpl->configuration
          "Transforms the ConfigurationTemplate into a Configuration resource."
          :service)

;; default implementation just updates the resourceURI
(defmethod tpl->configuration :default
  [resource]
  (assoc resource :resourceURI resource-uri))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a ConfigurationTemplate to create new Configuration
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        body (-> body
                 (assoc :resourceURI create-uri)
                 (std-crud/resolve-hrefs idmap)
                 (crud/validate)
                 (:configurationTemplate)
                 (tpl->configuration))]
    (add-impl (assoc request :body body))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def edit-impl (std-crud/edit-fn resource-name))

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))

;;
;; use name as the identifier
;;

(defmethod crud/new-identifier resource-name
  [resource resource-name]
  (if-let [new-id (:service resource)]
    (assoc resource :id (str (u/de-camelcase resource-name) "/" new-id))))
