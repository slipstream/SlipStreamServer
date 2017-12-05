(ns com.sixsq.slipstream.ssclj.resources.callback
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.callback]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]))

(def ^:const resource-tag :callbacks)

(def ^:const resource-name "Callback")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "CallbackCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate subclasses of sessions
;;


(def validate-fn (u/create-spec-validation-fn :cimi/callback))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

;;
;; multimethod for ACLs
;;

(defn create-acl []
  {:owner {:principal "ADMIN"
           :type      "ROLE"}
   :rules [{:principal "ADMIN"
            :type      "ROLE"
            :right     "VIEW"}]})

(defmethod crud/add-acl resource-uri
  [{:keys [acl] :as resource} request]
  (assoc
    resource
    :acl
    (or acl (create-acl))))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))
(defmethod crud/add resource-name
  [request]
  (add-impl
    (if-not (get-in request [:body :state])
      (assoc-in request [:body :state] "WAITING")
      request)))

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
;; available operations
;;


(defmethod crud/set-operations resource-uri
  [{:keys [id resourceURI] :as resource} request]
  (let [href (str id "/join")]
    (try
      (a/can-modify? resource request)
      (let [ops (if (.endsWith resourceURI "Collection")
                  [{:rel (:add c/action-uri) :href id}]
                  [{:rel (:delete c/action-uri) :href id}
                   {:rel (:validate c/action-uri) :href href}])]
        (assoc resource :operations ops))
      (catch Exception e
        (if (.endsWith resourceURI "Collection")
          (dissoc resource :operations)
          (assoc resource :operations [{:rel (:validate c/action-uri) :href href}]))))))

;;
;; actions
;;

(defn dispatch-conversion
  "Dispatches on the Action for multimethods that take the resource as arguments."
  [resource]
  (:action resource))

(defmulti validate-action-callback dispatch-conversion)

(defmethod validate-action-callback :default
  [resource]
  (log-util/log-and-throw 400 (str "error executing join callback: '" (dispatch-conversion resource) "'")))

(defmethod crud/do-action [resource-url "validate"]
  [{{uuid :uuid} :params}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (crud/retrieve-by-id id {:user-name  "INTERNAL"
                                   :user-roles [id]})       ;; Essentially turn off authz by spoofing owner of resource.
          (validate-action-callback)))
    (catch ExceptionInfo ei
      (ex-data ei))))
