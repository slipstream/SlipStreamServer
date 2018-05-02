(ns com.sixsq.slipstream.ssclj.resources.user-params
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.user-params-exec]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params.impl :as cpi]
    [clojure.string :as s]))

(def ^:const resource-tag :userParam)

(def ^:const resource-name "UserParam")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "UserParamsCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate subclasses of UserParameters
;;

(defmulti validate-subtype :paramsType)

(defmethod validate-subtype :default
  [resource]
  (let [err-msg (str "unknown UserParam type: " (:paramsType resource))]
    (throw
      (ex-info err-msg {:status  400
                        :message err-msg
                        :body    resource}))))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of UserParameters
;;

(defn dispatch-on-cloud-service-type [resource]
  (get-in resource [:userParamTemplate :paramsType]))

(defmulti create-validate-subtype dispatch-on-cloud-service-type)

(defmethod create-validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown UserParam create type: " (dispatch-on-cloud-service-type resource)) resource)))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))

;;
;; multimethod for ACLs
;;

(defn create-acl
  [id]
  {:owner {:principal id
           :type      "USER"}
   :rules [{:principal id
            :type      "USER"
            :right     "MODIFY"}
           {:principal "ADMIN"
            :type      "ROLE"
            :right     "ALL"}]})

(defmethod crud/add-acl resource-uri
  [{:keys [acl] :as resource} request]
  (if acl
    resource
    (let [user-id (:identity (a/current-authentication request))]
      (assoc resource :acl (create-acl user-id)))))


;;
;; template processing
;;

(defmulti tpl->user-params
          "Transforms the UserParamTemplate into a UserParams resource."
          :paramsType)

;; default implementation just updates the resourceURI
(defmethod tpl->user-params :default
  [resource]
  (assoc resource :resourceURI resource-uri))

;;
;; CRUD operations
;;

(defn- resource-ids-str-from-resp
  [resp]
  (->> (get-in resp [:body resource-tag])
       (map #(:id %))
       (s/join ", ")))

(defn conflict-if-exists
  "Only one :paramsType document is allowed per user."
  [request]
  (let [params-type (get-in request [:body :userParamTemplate :paramsType])
        cimi-filter (cpi/cimi-filter {"$filter" (format "paramsType='%s'" params-type)})
        req         (update-in request [:cimi-params] #(assoc % :filter cimi-filter))
        resp        (crud/query req)
        count       (-> resp
                        (get :body {:count 0})
                        (get :count 0))]
    (when (> count 0)
      (logu/log-and-throw 409
        (format "Resource of type %s for '%s' already exists: %s" params-type
                (:user-name request) (resource-ids-str-from-resp resp))))))

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a UserParamTemplate to create new UserParams
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (conflict-if-exists request)
  (let [idmap {:identity (:identity request)}
        body  (-> body
                  (assoc :resourceURI create-uri)
                  (crud/validate)
                  (:userParamTemplate)
                  (tpl->user-params request))]
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
;; initialization: no schema for this parent resource
;;
(defn initialize
  []
  (std-crud/initialize resource-url nil))
