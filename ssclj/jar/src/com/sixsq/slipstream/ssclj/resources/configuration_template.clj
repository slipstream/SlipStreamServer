(ns com.sixsq.slipstream.ssclj.resources.configuration-template
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.configuration-template.spec :as schema]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-tag :configurationTemplates)

(def ^:const resource-name "ConfigurationTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ConfigurationTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ADMIN"
                            :type      "ROLE"
                            :right     "VIEW"}]})

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; atom to keep track of the loaded ConfigurationTemplate resources
;;
(def templates (atom {}))
(def descriptions (atom {}))

(defn collection-wrapper-fn
  "Specialized version of this function that removes the adding
   of operations to the collection and entries.  These are already
   part of the stored resources."
  [resource-name collection-acl collection-uri collection-key]
  (fn [_ entries]
    (let [skeleton {:acl         collection-acl
                    :resourceURI collection-uri
                    :id          (u/de-camelcase resource-name)}]
      (assoc skeleton collection-key entries))))

(defn complete-resource
  "Completes the given document with server-managed information:
   resourceURI, timestamps, operations, and ACL."
  [{:keys [service] :as resource}]
  (when service
    (let [id (str resource-url "/" service)
          href (str id "/describe")
          ops [{:rel (:describe c/action-uri) :href href}]]
      (-> resource
          (merge {:id          id
                  :resourceURI resource-uri
                  :acl         resource-acl
                  :operations  ops})
          u/update-timestamps))))

(defn register
  "Registers a given ConfigurationTemplate resource and its description
   with the server.  The resource document (resource) and the description
   (desc) must be valid.  The key will be used to create the id of
   the resource as 'configuration-template/key'."
  [resource desc]
  (when-let [full-resource (complete-resource resource)]
    (let [id (:id full-resource)]
      (swap! templates assoc id full-resource)
      (log/info "loaded ConfigurationTemplate" id)
      (when desc
        (let [acl (:acl full-resource)
              full-desc (assoc desc :acl acl)]
          (swap! descriptions assoc id full-desc))
        (log/info "loaded ConfigurationTemplate description" id)))))

;;
;; schemas
;;

(def ConfigurationTemplateAttrs
  {:service c/NonBlankString})

(def ConfigurationTemplate
  (merge c/CommonAttrs
         c/AclAttr
         ConfigurationTemplateAttrs))

(def ConfigurationTemplateRef
  (s/constrained
    (merge ConfigurationTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    seq 'not-empty?))

(def ConfigurationTemplateDescription
  (merge c/CommonParameterDescription
         {:service {:displayName "Service"
                    :category    "general"
                    :description "identifies the service to be configured"
                    :type        "string"
                    :mandatory   true
                    :readOnly    true
                    :order       0}}))
;;
;; validation
;;

(def validate-fn (u/create-spec-validation-fn ::schema/configuration-template))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

;;
;; CRUD operations
;;

(defmethod crud/add resource-name
  [request]
  (throw (u/ex-bad-method request)))

(defmethod crud/retrieve resource-name
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (get @templates id)
          (a/can-view? request)
          (u/json-response)))
    (catch ExceptionInfo ei
      (ex-data ei))))

;; must override the default implementation so that the
;; data can be pulled from the atom rather than the database
(defmethod crud/retrieve-by-id resource-url
  [id]
  (try
    (get @templates id)
    (catch ExceptionInfo ei
      (ex-data ei))))

(defmethod crud/edit resource-name
  [request]
  (throw (u/ex-bad-method request)))

(defmethod crud/delete resource-name
  [request]
  (throw (u/ex-bad-method request)))

(defmethod crud/query resource-name
  [request]
  (a/can-view? {:acl collection-acl} request)
  (let [wrapper-fn (collection-wrapper-fn resource-name collection-acl collection-uri resource-tag)
        ;; FIXME: At least the paging options should be supported.
        options (select-keys request [:identity :query-params :cimi-params :user-name :user-roles])
        [count-before-pagination entries] ((juxt count vals) @templates)
        wrapped-entries (wrapper-fn request entries)
        entries-and-count (assoc wrapped-entries :count count-before-pagination)]
    (u/json-response entries-and-count)))

;;
;; actions
;;
(defmethod crud/do-action [resource-url "describe"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (get @descriptions id)
          (a/can-view? request)
          (u/json-response)))
    (catch ExceptionInfo ei
      (ex-data ei))))


