(ns com.sixsq.slipstream.ssclj.resources.callback
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.spec.callback]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [com.sixsq.slipstream.util.response :as r])
  (:import (clojure.lang ExceptionInfo)))

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

(def dispatch-on-action :action)


(defmulti execute dispatch-on-action)


(defmethod execute :default
  [{:keys [id] :as resource}]
  (utils/callback-failed! id)
  (log-util/log-and-throw 400 (str "error executing callback: '" (dispatch-on-action resource) "'")))


(defmethod crud/do-action [resource-url "execute"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (when-let [callback (crud/retrieve-by-id id {:user-name "INTERNAL", :user-roles ["ADMIN"]})]
        (if (utils/executable? callback)
          (execute callback)
          (r/map-response "cannot re-execute callback" 409 id))))
    (catch ExceptionInfo ei
      (ex-data ei))))
