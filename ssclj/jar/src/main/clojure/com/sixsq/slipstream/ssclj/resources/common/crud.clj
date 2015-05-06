(ns com.sixsq.slipstream.ssclj.resources.common.crud
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; CRUD dispatch functions
;;

(defn resource-name-dispatch
  [request]
  (get-in request [:params :resource-name]))

(defn resource-name-and-action-dispatch
  [request]
  (-> request
      :params
      (juxt :resource-name :action)))

;;
;; Primary CRUD multi-methods
;;

(defmulti add resource-name-dispatch)

(defmethod add :default
  [request]
  (throw (u/ex-bad-method request)))


(defmulti query resource-name-dispatch)

(defmethod query :default
  [request]
  (throw (u/ex-bad-method request)))


(defmulti retrieve resource-name-dispatch)

(defmethod retrieve :default
  [request]
  (throw (u/ex-bad-method request)))

(defmulti edit resource-name-dispatch)

(defmethod edit :default
  [request]
  (throw (u/ex-bad-method request)))


(defmulti delete resource-name-dispatch)

(defmethod delete :default
  [request]
  (throw (u/ex-bad-method request)))

(defmulti do-action resource-name-and-action-dispatch)

(defmethod do-action :default
  [request]
  (throw (u/ex-bad-method request)))

;;
;; Resource schema validation.
;;

(defmulti validate
          "Validates the given resource, returning the resource itself on success.
           This method dispatches on the value of resourceURI.  For any unknown
           dispatch value, the method throws an exception."
          :resourceURI)

(defmethod validate :default
           [resource]
  (throw (ex-info (str "unknown resource type: " (:resourceURI resource)) resource)))

;;
;; Provide allowed operations for resources and collections
;;

(defmulti set-operations
          "Adds the authorized resource operations to the resource based on the current
           user and the resource's ACL.  Dispatches on the value of resourceURI.
           For any unregistered resourceURI, the default implementation will add the
           'add' action for a Collection and the 'edit' and 'delete' actions for resources,
           if the current user has the MODIFY right."
          :resourceURI)

(defmethod set-operations :default
  [resource request]
  (try
    (a/can-modify? resource request)
    (let [href (:id resource)
          resourceURI (:resourceURI resource)
          ops (if (.endsWith resourceURI "Collection")
                [{:rel (:add c/action-uri) :href href}]
                [{:rel (:edit c/action-uri) :href href}
                 {:rel (:delete c/action-uri) :href href}])]
      (assoc resource :operations ops))
    (catch Exception e
      (dissoc resource :operations))))

;;
;; Determine the identifier for a new resource.
;; This is normally a random UUID, but may require
;; specialization, for example using the username for
;; user resources.
;;

(defmulti new-identifier
  (fn [json resource-name]
    resource-name))

(defmethod new-identifier :default
  [json resource-name]
  (assoc json :id (u/new-resource-id resource-name)))

;;
;; Determine the ACL to use for a new resource.
;; The default is to leave the :acl key blank.
;;

(defmulti add-acl
  (fn [{:keys [resourceURI]} request]
    resourceURI))

(defmethod add-acl :default
  [json request]
  json)
