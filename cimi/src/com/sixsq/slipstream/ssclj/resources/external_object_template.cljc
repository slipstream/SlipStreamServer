(ns com.sixsq.slipstream.ssclj.resources.external-object-template
  (:require [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
            [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
            [clojure.tools.logging :as log]
            [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
            [com.sixsq.slipstream.util.response :as r]
            [com.sixsq.slipstream.auth.acl :as a])
  (:import (clojure.lang ExceptionInfo)))


(def ^:const resource-tag :externalObjectTemplates)

(def ^:const resource-name "ExternalObjectTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ExternalObjectTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

; only authenticated users can view and create external objects
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(def desc-acl {:owner {:principal "ADMIN"
                       :type      "ROLE"}
               :rules [{:principal "ANON"
                        :type      "ROLE"
                        :right     "VIEW"}]})

;;
;; atom to keep track of the ExternalObjectTemplate descriptions
;;
(def descriptions (atom {}))

(defn register
  "Registers a given ExternalObjectTemplate description with the server. The
   description (desc) must be valid. The object type must be used as
   the id. The description can be looked up via the id, e.g. 'report'."
  [id desc]
  (when (and id desc)
    (let [full-desc (assoc desc :acl desc-acl)]
      (swap! descriptions assoc id full-desc))
    (log/info "loaded ExternalObjectTemplate description" id)))

(def ExternalObjectTemplateDescription
  (merge c/CommonParameterDescription
         {:type      {:displayName "External Object type"
                        :category    "general"
                        :description "type of external object"
                        :type        "string"
                        :mandatory   true
                        :readOnly    true
                        :order       10}
          :instance    {:displayName "External methode type key (Name)"
                        :category    "general"
                        :description "key used to identify this type of external object"
                        :type        "string"
                        :mandatory   true
                        :readOnly    true
                        :order       11}
          :uri       {:displayName "S3 bucket location"
                        :category    "general"
                        :description "optional path to S3 bucket where the object is stored"
                        :type        "string"
                        :mandatory   false
                        :readOnly    false
                        :order       12}
          :state {:displayName "External object state"
                        :category    "general"
                        :description "optional state of the external object"
                        :type        "enum"
                        :mandatory   false
                        :readOnly    false
                        :order       13
                        :enum        ["new" "stored"]}}))

;;
;; multimethods for validation
;;

(defmulti validate-subtype
          "Validates the given resource against the specific
           ExternalObjectTemplate subtype schema."
          :type)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown ExternalObjectTemplate type: " (:type resource)) resource)))

(defmethod crud/validate
  resource-uri
  [resource]
  (validate-subtype resource))


;;
;; identifiers for these resources are the same as the :instance value
;;
(defmethod crud/new-identifier resource-name
  [{:keys [instance] :as resource} resource-name]
  (->> instance
       (str resource-url "/")
       (assoc resource :id)))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [{{:keys [type]} :body :as request}]
  (if (get @descriptions type)
    (add-impl request)
    (throw (r/ex-bad-request (str "invalid external object type '" type "'")))))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))

;;
;; actions
;;
(defmethod crud/do-action [resource-url "describe"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (get @descriptions id)
          (a/can-view? request)
          (r/json-response)))
    (catch ExceptionInfo ei
      (ex-data ei))))
