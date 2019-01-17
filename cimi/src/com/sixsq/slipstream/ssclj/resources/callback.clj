(ns com.sixsq.slipstream.ssclj.resources.callback
  "
Deferred actions that must be triggered by a user or other external agent.
For example, used for email validation.

Each callback represents a single, atomic action that must be triggered by an
external agent. The action is identified by the `action` attribute. Some
actions may require state information, which may be provided in the `data`
attribute.

All callback resources support the CIMI `execute` action, which triggers the
action of the callback. The state of the callback will indicate the success or
failure of the action.

Generally, these resources are created by CIMI server resources rather than
end-users. Anyone with the URL of the callback can trigger the `execute`
action. Consequently, the callback id should only be communicated to
appropriate users.
"
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.callback :as callback]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [com.sixsq.slipstream.util.response :as r]))

(def ^:const resource-tag :callbacks)

(def ^:const resource-name "Callback")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "CallbackCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate subclasses of callbacks
;;

(def validate-fn (u/create-spec-validation-fn ::callback/schema))
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
  (add-impl (assoc-in request [:body :state] "WAITING")))


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
  (let [href (str id "/execute")
        collection? (u/cimi-collection? resourceURI)
        modifiable? (a/modifiable? resource request)
        ops (cond-> []
                    (and collection? modifiable?) (conj {:rel (:add c/action-uri) :href id})
                    (and (not collection?) modifiable?) (conj {:rel (:delete c/action-uri) :href id})
                    (and (not collection?) (utils/executable? resource)) (conj {:rel (:execute c/action-uri) :href href}))]
    (if (empty? ops)
      (dissoc resource :operations)
      (assoc resource :operations ops))))

;;
;; actions
;;

(defn action-dispatch
  [callback-resource request]
  (:action callback-resource))


(defmulti execute action-dispatch)


(defmethod execute :default
  [{:keys [id] :as callback-resource} request]
  (utils/callback-failed! id)
  (let [msg (format "error executing callback: '%s' of type '%s'" id (action-dispatch callback-resource request))]
    (log-util/log-and-throw 400 msg)))


(defmethod crud/do-action [resource-url "execute"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (when-let [callback-resource (crud/retrieve-by-id-as-admin id)]
        (if (utils/executable? callback-resource)
          (execute callback-resource request)
          (r/map-response "cannot re-execute callback" 409 id))))
    (catch Exception e
      (or (ex-data e) (throw e)))))


;;
;; general utility for creating a new callback in other resources
;;

;; FIXME: Fix ugliness around needing to create ring requests with authentication!
(defn create
  "Creates a callback resource with the given action-name, baseURI, target
   resource, data (optional). Returns the URL to trigger the callback's action."
  ([action-name baseURI href]
   (create action-name baseURI href nil))
  ([action-name baseURI href data]
   (let [callback-request {:params   {:resource-name resource-url}
                           :body     (cond-> {:action         action-name
                                              :targetResource {:href href}}
                                             data (assoc :data data))
                           :identity {:current         "INTERNAL"
                                      :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                    :roles    ["ADMIN"]}}}}
         {{:keys [resource-id]} :body status :status} (crud/add callback-request)]

     (if (= 201 status)
       (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id-as-admin resource-id) {})]
         (if-let [validate-op (u/get-op callback-resource "execute")]
           (str baseURI validate-op)
           (let [msg "callback does not have execute operation"]
             (throw (ex-info msg (r/map-response msg 500 resource-id)))))
         (let [msg "cannot retrieve user create callback"]
           (throw (ex-info msg (r/map-response msg 500 resource-id)))))
       (let [msg "cannot create user callback"]
         (throw (ex-info msg (r/map-response msg 500 ""))))))))


;;
;; initialization: common schema for all subtypes
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::callback/schema))
