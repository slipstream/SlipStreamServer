(ns com.sixsq.slipstream.ssclj.resources.connector-template
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-tag :connectorTemplates)

(def ^:const resource-name "ConnectorTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ConnectorTemplateCollection")

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
;; atom to keep track of the loaded ConnectorTemplate resources
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
  [{:keys [cloudServiceType] :as resource}]
  (when cloudServiceType
    (let [id (str resource-url "/" cloudServiceType)
          href (str id "/describe")
          ops [{:rel (:describe c/action-uri) :href href}]]
      (-> resource
          (merge {:id id
                  :resourceURI resource-uri
                  :acl resource-acl
                  :operations ops})
          u/update-timestamps))))

(defn register
  "Registers a given ConnectorTemplate resource and its description
   with the server.  The resource document (resource) and the description
   (desc) must be valid.  The key will be used to create the id of
   the resource as 'connector-template/key'."
  [resource desc]
  (when-let [full-resource (complete-resource resource)]
    (let [id (:id full-resource)]
      (swap! templates assoc id full-resource)
      (log/info "loaded ConnectorTemplate" id)
      (when desc
        (let [acl (:acl full-resource)
              full-desc (assoc desc :acl acl)]
          (swap! descriptions assoc id full-desc))
        (log/info "loaded ConnectorTemplate description" id)))))

;;
;; schemas
;;

(def ConnectorTemplateAttrs
  {:cloudServiceType c/NonBlankString})

(def ConnectorTemplate
  (merge c/CommonAttrs
         c/AclAttr
         ConnectorTemplateAttrs))

(def ConnectorTemplateRef
  (s/constrained
    (merge ConnectorTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    seq 'not-empty?))

(def ConnectorTemplateDescription
  (merge c/CommonParameterDescription
         {:cloudServiceType {:displayName "Cloud Service Type"
                             :category    "general"
                             :description "type of cloud service targeted by connector"
                             :type        "string"
                             :mandatory   true
                             :readOnly    true
                             :order       0}}))
;;
;; multimethods for validation
;;

(defmulti validate-subtype
          "Validates the given resource against the specific
           ConnectorTemplate subtype schema."
          :cloudServiceType)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown ConnectorTemplate type: " (:cloudServiceType resource)) resource)))

(defmethod crud/validate
  resource-uri
  [resource]
  (validate-subtype resource))

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


