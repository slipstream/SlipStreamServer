(ns com.sixsq.slipstream.ssclj.resources.external-object
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.util.response :as r])
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
;; multimethods for validation for the external objects
;;

(defmulti validate-subtype
          "Validates the given resource against the specific
           ExternalObjectTemplate subtype schema."
          :objectType)


(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown ExternalObjectTemplate type: " (:objectType resource)) resource)))


(defmethod crud/validate
  resource-uri
  [resource]
  (validate-subtype resource))

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

(defn standard-external-object-collection-operations
  [{:keys [id] :as resource} request]
  (when (a/authorized-modify? resource request)
    [{:rel (:add c/action-uri) :href id}]))

(defn standard-external-object-resource-operations
  [{:keys [id state] :as resource} request]
  (let [viewable? (a/authorized-view? resource request)
        modifiable? (a/authorized-modify? resource request)
        new? (= state-new state)
        ready? (= state-ready state)
        ops (cond-> []
                    modifiable? (conj {:rel (:delete c/action-uri) :href id})
                    (and new? modifiable?) (conj {:rel (:upload c/action-uri) :href (str id "/upload")})
                    (and ready? viewable?) (conj {:rel (:download c/action-uri) :href (str id "/download")}))]
    (when (seq ops)
      (vec ops))))

(defn standard-external-object-operations
  "Provides a list of the standard external object operations, depending
   on the user's authentication and whether this is a ExternalObject or
   a ExternalObjectCollection."
  [{:keys [resourceURI] :as resource} request]
  (if (.endsWith resourceURI "Collection")
    (standard-external-object-collection-operations resource request)
    (standard-external-object-resource-operations resource request)))

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
(defn check-cred-exists
  [body idmap]
  (let [href (get-in body [:externalObjectTemplate :objectStoreCred])]
    (std-crud/resolve-hrefs href idmap))
  body)

(defn resolve-hrefs
  [body idmap]
  (let [os-cred-href (if (contains? (:externalObjectTemplate body) :objectStoreCred)
                       {:objectStoreCred (get-in body [:externalObjectTemplate :objectStoreCred])}
                       {})]                                 ;; to put back the unexpanded href after
    (-> body
        (check-cred-exists idmap)
        ;; remove connector href (if any); regular user MAY NOT have rights to see it
        (update-in [:externalObjectTemplate] dissoc :objectStoreCred)
        (std-crud/resolve-hrefs idmap)
        ;; put back unexpanded connector href
        (update-in [:externalObjectTemplate] merge os-cred-href))))

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a ExternalObjectTemplate to create new ExternalObject
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        body (-> body
                 (assoc :resourceURI create-uri)
                 (resolve-hrefs idmap)
                 (crud/validate)
                 (:externalObjectTemplate)
                 (tpl->externalObject)
                 (assoc :state state-new))]
    (add-impl (assoc request :body body))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))
(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))


(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))

;; URL request operations utils

(defn expand-cred
  "Returns credential document after expanding `href-obj-store-cred` credential href."
  [href-obj-store-cred request]
  (std-crud/resolve-hrefs href-obj-store-cred {:identity (:identity request)} true))

(defn connector-from-cred
  "Returns connector document after expanding it from `cred` credential."
  [cred request]
  (-> cred
      (std-crud/resolve-hrefs {:identity (:identity request)} true)
      :connector))

(defn expand-obj-store-creds
  [href-obj-store-cred request]
  (let [{:keys [key secret] :as cred} (expand-cred href-obj-store-cred request)
        connector (connector-from-cred cred request)]
    {:key      key
     :secret   secret
     :endpoint (:objectStoreEndpoint connector)}))


;;; Upload URL operation

(def ex-msg-upload-bad-state "External object is not in new state to be uploaded!")

(defn upload-fn
  "Provided 'resource' and 'request', returns object storage upload URL."
  [{:keys [state contentType bucketName objectName objectStoreCred runUUID filename]} {{ttl :ttl} :body :as request}]
  (if (= state state-new)
    (let [object-name (if (not-empty objectName)
                        objectName
                        (format "%s/%s" runUUID filename))
          obj-store-conf (expand-obj-store-creds objectStoreCred request)]
      (log/info "Requesting upload url:" object-name)
      (s3/create-bucket! obj-store-conf bucketName)
      (s3/generate-url obj-store-conf bucketName object-name :put
                       {:ttl (or ttl s3/default-ttl) :content-type contentType :filename filename}))
    (logu/log-and-throw-400 ex-msg-upload-bad-state)))

(defmulti upload-subtype
          (fn [resource _] (:objectType resource)))

(defmethod upload-subtype :default
  [resource request]
  (try
    (a/can-modify? resource request)
    (let [upload-uri (upload-fn resource request)]
      (-> (assoc resource :state state-ready)
          (db/edit request))
      (r/json-response {:uri upload-uri}))
    (catch ExceptionInfo ei
      (ex-data ei))))

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

(def ex-msg-download-bad-state "External object is not in ready state to be downloaded!")

(defn download-fn
  "Provided 'resource' and 'request', returns object storage download URL."
  [{:keys [state contentType bucketName objectName objectStoreCred runUUID filename]} {{ttl :ttl} :body :as request}]
  (if (= state state-ready)
    (let [object-name (if (not-empty objectName)
                        objectName
                        (format "%s/%s" runUUID filename))
          obj-store-conf (expand-obj-store-creds objectStoreCred request)]
      (log/info "Requesting download url: " object-name)
      (s3/generate-url obj-store-conf bucketName object-name :get
                       {:ttl (or ttl s3/default-ttl) :content-type contentType :filename filename}))
    (logu/log-and-throw-400 ex-msg-download-bad-state)))

(defmulti download-subtype
          (fn [resource _] (:objectType resource)))

(defmethod download-subtype :default
  [resource request]
  (try
    (a/can-modify? resource request)
    (r/json-response {:uri (download-fn resource request)})
    (catch ExceptionInfo ei
      (ex-data ei))))

(defmethod crud/do-action [resource-url "download"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (crud/retrieve-by-id id {:user-name  "INTERNAL"
                                   :user-roles ["ADMIN"]})
          (download-subtype request)))
    (catch ExceptionInfo ei
      (ex-data ei))))

;;; Delete resource.

(def delete-impl (std-crud/delete-fn resource-name))

(defmulti delete-subtype
          (fn [resource _] (:objectType resource)))

(defmethod delete-subtype :default
  [{:keys [id bucketName objectStoreCred] :as resource} {{keep? :keep-s3-object} :body :as request}]
  (let [obj-name (cu/document-id id)]
    (when-not keep?
      (s3/delete-s3-object (expand-obj-store-creds objectStoreCred request) bucketName obj-name))
    (delete-impl request)))

(defmethod crud/delete resource-name
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (crud/retrieve-by-id id {:user-name  "INTERNAL"
                                   :user-roles ["ADMIN"]})
          (delete-subtype request)))
    (catch ExceptionInfo ei
      (ex-data ei))))

