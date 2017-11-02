(ns com.sixsq.slipstream.ssclj.resources.job
  (:require
    [superstring.core :as str]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.spec.job]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.util.response :as sr]
    [ring.util.response :as r]
    [com.sixsq.slipstream.db.impl :as db]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.resources.job.utils :as ju])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name "Job")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "JobCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(defn initialize []
  (ju/create-job-queue))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn :cimi/job))
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

(defn add-impl [{body :body :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (let [{:keys [id] :as new-job} (-> body
                                     u/strip-service-attrs
                                     (crud/new-identifier resource-name)
                                     (assoc :resourceURI resource-uri)
                                     (assoc :state ju/state-queued)
                                     u/update-timestamps
                                     ju/job-cond->addition
                                     (crud/add-acl request)
                                     crud/validate)]
    (ju/add-job-to-queue id)
    (db/add resource-name new-job {})))

(defmethod crud/add resource-name
  [request]
  (add-impl request))


(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(defn edit-impl [{{uuid :uuid} :params {:keys [statusMessage] :as body} :body :as request}]
  (try
    (let [current (-> (str (u/de-camelcase resource-name) "/" uuid)
                      (db/retrieve request)
                      (a/can-modify? request)
                      (cond-> statusMessage ju/update-timeOfStatusChange))
          merged (merge current body)]
      (-> merged
          u/update-timestamps
          ju/job-cond->edition
          crud/validate
          (db/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))


(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(defn delete-impl [{{uuid :uuid} :params :as request}]
  (try
    (-> (str (u/de-camelcase resource-name) "/" uuid)
        (db/retrieve request)
        (a/can-modify? request)
        (ju/stop)
        (db/delete request))
    (catch ExceptionInfo ei
      (ex-data ei))))


(defmethod crud/delete resource-name
  [request]
  (delete-impl request))


(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))


;;
;; provide an action that allows the job to be stoppable.
;;

(defmethod crud/set-operations resource-uri
  [{:keys [id resourceURI username] :as resource} request]
  (let [href (str id "/stop")
        collect-op {:rel (:stop c/action-uri) :href href}]
    (-> (crud/set-standard-operations resource request)
        (update-in [:operations] conj collect-op))))

(defmethod crud/do-action [resource-url "stop"]
  [{{uuid :uuid} :params :as request}]
  (try
    (-> (str (u/de-camelcase resource-name) "/" uuid)
        (db/retrieve request)
        (a/can-modify? request)
        (ju/stop)
        (db/edit request))
    (catch ExceptionInfo ei
      (ex-data ei))))




