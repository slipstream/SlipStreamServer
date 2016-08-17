(ns com.sixsq.slipstream.ssclj.resources.connector
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-tag :connectors)

(def ^:const resource-name "Connector")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ConnectorCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})
;;
;; schemas
;;

;; FIXME: Need to dynamically load connector schema definitions
(def Connector
  (merge c/CommonAttrs
         c/AclAttr
         {:serviceName c/NonBlankString}))

(def ConnectorCreate
  (merge c/CreateAttrs
         {:connectorTemplate tpl/ConnectorTemplateRef}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Connector))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn ConnectorCreate))
(defmethod crud/validate create-uri
  [resource]
  (create-validate-fn resource))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; template processing
;;

;; FIXME: needs to be multi-function to handle different connectors
(defn tpl->connector
  [tpl]
  (std-crud/resolve-hrefs tpl))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a ConnectorTemplate to create new license
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [body (-> body
                 (assoc :resourceURI create-uri)
                 (crud/validate)
                 (:connectorTemplate)
                 (tpl->connector))]
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
