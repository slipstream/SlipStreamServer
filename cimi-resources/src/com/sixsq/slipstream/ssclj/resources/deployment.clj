(ns com.sixsq.slipstream.ssclj.resources.deployment
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment :as d]
    [com.sixsq.slipstream.util.response :as r]))

(def ^:const resource-tag :deployments)

(def ^:const resource-name "Deployment")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "DeploymentCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate deployment
;;

(def validate-fn (u/create-spec-validation-fn ::d/deployment))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))


;;
;; multimethod for ACLs
;;


(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))


;;
;; CRUD operations
;;


(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (add-impl (assoc request :body body)))


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
;; actions may be needed by certain authentication methods (notably external
;; methods like GitHub and OpenID Connect) to validate a given session
;;

(defmethod crud/set-operations resource-uri
  [{:keys [id] :as resource} request]
  (let [href (str id "/start")
        start-op {:rel (:start c/action-uri) :href href}
        stop-op {:rel (:stop c/action-uri) :href href}]
    (-> (crud/set-standard-operations resource request)
        (update :operations conj start-op stop-op))))


(defmethod crud/do-action [resource-url "start"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (r/map-response (str "starting " id) 202 id "job/created-start-job")) ;; FIXME: Actually do something, create start job
    (catch Exception e
      (or (ex-data e) (throw e)))))

(defmethod crud/do-action [resource-url "stop"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (r/map-response (str "stopping " id) 202 id "job/created-stop-job")) ;; FIXME: Actually do something, create stop job
    (catch Exception e
      (or (ex-data e) (throw e)))))


;;
;; initialization: no schema for this parent resource
;;
(defn initialize
  []
  (std-crud/initialize resource-url nil))
