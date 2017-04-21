(ns com.sixsq.slipstream.ssclj.resources.credential-template
  (:require
    [clojure.spec :as s]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-tag :credentialTemplates)

(def ^:const resource-name "CredentialTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "CredentialTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

;; the templates are managed as in-memory resources, so modification
;; of the collection is not permitted, but anonymous credentials must be
;; able to list and view templates (if anonymous registration is
;; permitted)
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; atom to keep track of the loaded CredentialTemplate resources
;;
(def templates (atom {}))
(def descriptions (atom {}))

(defn collection-wrapper-fn
  "Specialized version of this function that removes the adding
   of operations to the collection and entries.  These are already
   part of the stored resources."
  [resource-name collection-acl collection-uri collection-key]
  (fn [request entries]
    (let [skeleton {:acl         collection-acl
                    :resourceURI collection-uri
                    :id          (u/de-camelcase resource-name)}]
      (-> skeleton
          (crud/set-operations request)
          (assoc collection-key entries)))))

(defn complete-resource
  "Completes the given document with server-managed information:
   resourceURI, timestamps, and operations.  NOTE: The subtype
   MUST provide an ACL for the template."
  [{:keys [type] :as resource}]
  (when type
    (let [id (str resource-url "/" type)
          href (str id "/describe")
          ops [{:rel (:describe c/action-uri) :href href}]]
      (-> resource
          (merge {:id          id
                  :resourceURI resource-uri
                  :operations  ops})
          u/update-timestamps))))

(defn register
  "Registers a given CredentialTemplate resource and its description
   with the server.  The resource document (resource) and the description
   (desc) must be valid.  The key will be used to create the id of
   the resource as 'credential-template/key'."
  [resource desc]
  (when-let [{:keys [id] :as full-resource} (complete-resource resource)]
    (swap! templates assoc id full-resource)
    (log/info "loaded CredentialTemplate" id)
    (when desc
      (let [acl (:acl full-resource)
            full-desc (assoc desc :acl acl)]
        (swap! descriptions assoc id full-desc))
      (log/info "loaded CredentialTemplate description" id))))

;;
;; schemas
;;

(def CredentialTemplateDescription
  (merge c/CommonParameterDescription
         {:type {:displayName "Credential Type"
                 :category    "general"
                 :description "type of credential"
                 :type        "string"
                 :mandatory   true
                 :readOnly    true
                 :order       0}}))
;;
;; multimethods for validation
;;

(defmulti validate-subtype
          "Validates the given resource against the specific
           CredentialTemplate subtype schema."
          :type)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown CredentialTemplate type: " (:type resource)) resource)))

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

(defn- viewable? [request {:keys [acl] :as entry}]
  (try
    (a/can-view? {:acl acl} request)
    (catch Exception _
      false)))

(defmethod crud/query resource-name
  [request]
  (a/can-view? {:acl collection-acl} request)
  (let [wrapper-fn (collection-wrapper-fn resource-name collection-acl collection-uri resource-tag)
        entries (or (filter (partial viewable? request) (vals @templates)) [])
        ;; FIXME: At least the paging options should be supported.
        options (select-keys request [:identity :query-params :cimi-params :credential-name :credential-roles])
        count-before-pagination (count entries)
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


