(ns com.sixsq.slipstream.ssclj.resources.connector
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.connector]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.util.response :as sr]))

(def ^:const resource-tag :connectors)

(def ^:const resource-name "Connector")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ConnectorCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate subclasses of connectors
;;

(defmulti validate-subtype
          :cloudServiceType)

(defmethod validate-subtype :default
  [resource]
  (let [err-msg (str "unknown Connector type: " (:cloudServiceType resource))]
    (throw
      (ex-info err-msg {:status  400
                        :message err-msg
                        :body    resource}))))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of connectors
;;

(defn dispatch-on-cloud-service-type [resource]
  (get-in resource [:connectorTemplate :cloudServiceType]))

(defmulti create-validate-subtype dispatch-on-cloud-service-type)

(defmethod create-validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown Connector create type: " (dispatch-on-cloud-service-type resource)) resource)))

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

(defmulti tpl->connector
          "Transforms the ConnectorTemplate into a Connector resource."
          :cloudServiceType)

;; default implementation just updates the resourceURI
(defmethod tpl->connector :default
  [resource]
  (assoc resource :resourceURI resource-uri))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a ConnectorTemplate to create new Connector
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        body (-> body
                 (assoc :resourceURI create-uri)
                 (std-crud/resolve-hrefs idmap)
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

;;
;; use name as the identifier
;;

(defmethod crud/new-identifier resource-name
  [resource resource-name]
  (if-let [new-id (:instanceName resource)]
    (assoc resource :id (str (u/de-camelcase resource-name) "/" new-id))))

;;; Activate operation

(defmulti activate-subtype
          :cloudServiceType)

(defmethod activate-subtype :default
  [resource]
  (let [err-msg (str "unknown Connector type: " (:cloudServiceType resource))]
    (throw
      (ex-info err-msg {:status  400
                        :message err-msg
                        :body    resource}))))

(defmethod crud/do-action [resource-url "activate"]
  [{{uuid :uuid} :params :as request}]
  (let [id (str resource-url "/" uuid)
        resource (crud/retrieve-by-id id {:user-name  "INTERNAL"
                                          :user-roles ["ADMIN"]})]
    (-> (activate-subtype resource)
        sr/json-response)))

(defmulti set-subtype-ops
          (fn [resource _] (:cloudServiceType resource)))

(defmethod set-subtype-ops :default
  [resource request]
  (crud/set-standard-operations resource request)
  )

(defmethod crud/set-operations resource-uri
  [resource request]
  (set-subtype-ops resource request))
