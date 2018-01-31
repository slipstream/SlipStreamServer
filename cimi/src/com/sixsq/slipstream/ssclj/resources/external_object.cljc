(ns com.sixsq.slipstream.ssclj.resources.external-object
  (:require [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
            [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
            [com.sixsq.slipstream.auth.acl :as a]
            [com.sixsq.slipstream.db.impl :as db]
            [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]))

(def ^:const resource-tag :externalObjects)

(def ^:const resource-name "ExternalObject")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ExternalObjectCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate subclasses of externalObject
;;

(defmulti validate-subtype
          :type)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown External object type: '" (:type resource) "'") resource)))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of external objects
;;

(defn dispatch-on-object-type [resource]
  (get-in resource [:externalObjectTemplate :type]))

(defmulti create-validate-subtype dispatch-on-object-type)

(defmethod create-validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown External Session create type: " (dispatch-on-object-type resource) resource) resource)))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))


;;
;; multimethod for ACLs
;;

(defn create-acl
  [id]
  {:owner {:principal id
           :type      "ROLE"}
   :rules [{:principal "ADMIN"
            :type      "ROLE"
            :right     "VIEW"}]})

(defmethod crud/add-acl resource-uri
  [{:keys [id acl] :as resource} request]
  (assoc
    resource
    :acl
    (or acl (create-acl id))))

(defn dispatch-conversion
  "Dispatches on the External object type for multimethods
   that take the resource and request as arguments."
  [resource _]
  (:type resource))

(defn standard-external-object-operations
  "Provides a list of the standard external object operations, depending
   on the user's authentication and whether this is an ExternalObject or
   a ExternalObjectCollection."
  [{:keys [id resourceURI] :as resource} request]
  (try
    (a/can-modify? resource request)
    (if (.endsWith resourceURI "Collection")
      [{:rel (:add c/action-uri) :href id}]
      [{:rel (:delete c/action-uri) :href id}])
    (catch Exception _
      nil)))

;; Sets the operations for the given resources.  This is a
;; multi-method because different types of external object resources
;; may require different operations
(defmulti set-session-operations dispatch-conversion)

;; Default implementation adds the standard external object operations
;; by ALWAYS replacing the :operations value.  If there are no
;; operations, the key is removed from the resource.
(defmethod set-session-operations :default
  [resource request]
  (let [ops (standard-external-object-operations resource request)]
    (cond-> (dissoc resource :operations)
            (seq ops) (assoc :operations ops))))

;; Just triggers the ExternalObject-level multimethod for adding operations
;; to the External object resource.
(defmethod crud/set-operations resource-uri
  [resource request]
  (set-session-operations resource request))

;; template processing

(defmulti tpl->externalObject dispatch-conversion)

;; All concrete external objects types MUST provide an implementation of this
;; multimethod. The default implementation will throw an 'internal
;; server error' exception.
;;
(defmethod tpl->externalObject :default
  [resource request]
  [{:status 500, :message "invalid external object resource implementation"} nil])

;;
;; CRUD operations
;;

(defn add-impl [{:keys [id body] :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (db/add
    resource-name
    (-> body
        u/strip-service-attrs
        (assoc :id id)
        (assoc :resourceURI resource-uri)
        u/update-timestamps
        (crud/add-acl request)
        crud/validate)
    {}))

; requires a ExternalObjectTemplate to create new External object
;; requires a ConnectorTemplate to create new Connector
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        body (-> body
                 (assoc :resourceURI create-uri)
                 (std-crud/resolve-hrefs idmap)
                 (crud/validate)
                 (:externalObjectTemplate)
                 (tpl->externalObject))]
    (add-impl (assoc request :body body))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))
(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))


(def delete-impl (std-crud/delete-fn resource-name))
(defmethod crud/delete resource-name
  [request]
  (delete-impl request)
  )

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