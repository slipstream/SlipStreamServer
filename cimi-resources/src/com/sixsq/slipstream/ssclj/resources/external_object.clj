(ns com.sixsq.slipstream.ssclj.resources.external-object
  (:require [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
            [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
            [com.sixsq.slipstream.auth.acl :as a]
            [com.sixsq.slipstream.db.impl :as db]
            [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud])
  (:import (clojure.lang ExceptionInfo)))

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


(def ^:const state-new "new")
(def ^:const state-ready "ready")
;;
;; validate subclasses of externalObject
;;

(defmulti validate-subtype
          :objectType)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown External object type: '" (:objectType resource) "'") resource)))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of external objects
;;

(defn dispatch-on-object-type [resource]
  (get-in resource [:externalObjectTemplate :objectType]))

(defmulti create-validate-subtype dispatch-on-object-type)

(defmethod create-validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown External Object create type: " (dispatch-on-object-type resource) resource) resource)))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))


;;
;; multimethod for ACLs
;;

(defn create-acl [id]
  {:owner {:principal "ADMIN"
           :type      "ROLE"}
   :rules [{:principal id
            :type      "USER"
            :right     "MODIFY"}]})

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

(defmethod crud/add-acl resource-uri
  [{:keys [acl] :as resource} request]
  (if acl
    resource
    (let [user-id (:identity (a/current-authentication request))]
      (assoc resource :acl (create-acl user-id)))))

;;;;;;;;
(defn dispatch-conversion
  "Dispatches on the External object type for multimethods
   that take the resource and request as arguments."
  [resource _]
  (:objectType resource))

(defn standard-external-object-operations
  "Provides a list of the standard external object operations, depending
   on the user's authentication and whether this is a ExternalObject or
   a ExternalObjectCollection."
  [{:keys [id resourceURI] :as resource} request]
  (try
    (a/can-modify? resource request)
    (if (.endsWith resourceURI "Collection")
      [{:rel (:add c/action-uri) :href id}]
      [{:rel (:delete c/action-uri) :href id}
       {:rel (:upload c/action-uri) :href (str id "/upload")}
       {:rel (:download c/action-uri) :href (str id "/download")}
       ])
    (catch Exception _
      nil)))

;; Sets the operations for the given resources.  This is a
;; multi-method because different types of external object resources
;; may require different operations
(defmulti set-external-object-operations dispatch-conversion)

;; Default implementation adds the standard external object operations
;; by ALWAYS replacing the :operations value.  If there are no
;; operations, the key is removed from the resource.
(defmethod set-external-object-operations :default
  [resource request]
  (let [ops (standard-external-object-operations resource request)]
    (cond-> (dissoc resource :operations)
            (seq ops) (assoc :operations ops))))

;; Just triggers the Session-level multimethod for adding operations
;; to the Session resource.
(defmethod crud/set-operations resource-uri
  [resource request]
  (set-external-object-operations resource request))

;;
;; template processing
;;

(defmulti tpl->externalObject
          "Transforms the ExternalObjectTemplate into a ExternalObject resource."
          :objectType)

;; default implementation just updates the resourceURI
(defmethod tpl->externalObject :default
  [resource]
  (assoc resource :resourceURI resource-uri))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a ExternalObjectTemplate to create new ExternalObject
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


;;; Upload URL operation

(defmulti upload-subtype
          (fn [resource _] (:objectType resource)))

(defmethod upload-subtype :default
  [resource _]
  (let [err-msg (str "unknown External Object type: " (:objectType resource))]
    (throw (ex-info err-msg {:status  400
                             :message err-msg
                             :body    resource}))))

(defmethod crud/do-action [resource-url "upload"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (crud/retrieve-by-id id {:user-name  "INTERNAL"
                                   :user-roles ["ADMIN"]})
          (upload-subtype request)))
    (catch ExceptionInfo ei
      (ex-data ei))))

;;; Download URL operation

(defmulti download-subtype
          (fn [resource _] (:objectType resource)))

(defmethod download-subtype :default
  [resource _]
  (let [err-msg (str "unknown External Object type: " (:objectType resource))]
    (throw (ex-info err-msg {:status  400
                             :message err-msg
                             :body    resource}))))

(defmethod crud/do-action [resource-url "download"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (crud/retrieve-by-id id {:user-name  "INTERNAL"
                                   :user-roles ["ADMIN"]})
          (download-subtype request)))
    (catch ExceptionInfo ei
      (ex-data ei))))