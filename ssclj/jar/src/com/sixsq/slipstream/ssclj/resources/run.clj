(ns com.sixsq.slipstream.ssclj.resources.run
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.run]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [superstring.core :as str]
    [ring.util.response :as r]
    [com.sixsq.slipstream.util.response :as sr]
    [com.sixsq.slipstream.db.impl :as db]
    [clojure.tools.logging :as log]
    [clojure.core.async :as async]
    [clj-time.core :as time])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-name "Run")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "RunCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn :cimi/run))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

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
;; override the operations method to add describe action
;;

(defmethod crud/set-operations resource-uri
  [{:keys [id resourceURI] :as resource} request]
  (let [href (str id "/start")]
    (try
      (a/can-modify? resource request)
      (let [ops (if (.endsWith resourceURI "Collection")
                  [{:rel (:add c/action-uri) :href id}]
                  [{:rel (:edit c/action-uri) :href id}
                   {:rel (:delete c/action-uri) :href id}
                   {:rel (:start c/action-uri) :href href}])]
        (assoc resource :operations ops))
      (catch Exception e
        (if (.endsWith resourceURI "Collection")
          (dissoc resource :operations)
          (assoc resource :operations [{:rel (:start c/action-uri) :href href}]))))))

;;
;; actions
;;

(defmethod crud/do-action [resource-url "start"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [current (-> (str (u/de-camelcase resource-name) "/" uuid)
                      (db/retrieve request)
                      (a/can-modify? request))
          updated-run (assoc current :start-time (u/time-now))]
      (-> updated-run
          (u/update-timestamps)
          (crud/validate)
          (db/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))