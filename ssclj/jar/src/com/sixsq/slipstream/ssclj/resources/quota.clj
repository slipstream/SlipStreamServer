(ns com.sixsq.slipstream.ssclj.resources.quota
  (:require
    [superstring.core :as str]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.spec.quota]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.quota.utils :as quota-utils]
    [com.sixsq.slipstream.util.response :as sr]
    [ring.util.response :as r])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name "Quota")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "QuotaCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn :cimi/quota))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

;;
;; use default ACL method
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (add-impl request))


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
;; provide an action that allows the quota to be evaluated
;; returns the quota resource augmented with the keys
;; :currentAll (for all usage of the quota) and :currentUser
;; (for all usage of the quota by that user).
;;


(defmethod crud/set-operations resource-uri
  [{:keys [id resourceURI username] :as resource} request]
  (let [href (str id "/collect")
        collect-op {:rel (:collect c/action-uri) :href href}]
    (-> (crud/set-standard-operations resource request)
        (update-in [:operations] conj collect-op))))


(defmethod crud/do-action [resource-url "collect"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (crud/retrieve-by-id id {:user-name  "INTERNAL"
                                   :user-roles ["ADMIN"]})
          (quota-utils/collect request)
          sr/json-response))
    (catch ExceptionInfo ei
      (ex-data ei))))




