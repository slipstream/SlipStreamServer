(ns com.sixsq.slipstream.ssclj.resources.user-template
  (:require
    [clojure.spec.alpha :as s]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.util.response :as r])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-tag :userTemplates)

(def ^:const resource-name "UserTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "UserTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

;; the templates are managed as in-memory resources, so modification
;; of the collection is not permitted, but anonymous users must be
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
;; atom to keep track of the loaded UserTemplate resources
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
  [{:keys [method] :as resource}]
  (when method
    (let [id (str resource-url "/" method)
          href (str id "/describe")
          ops [{:rel (:describe c/action-uri) :href href}]]
      (-> resource
          (merge {:id          id
                  :resourceURI resource-uri
                  :operations  ops})
          u/update-timestamps))))

(defn register
  "Registers a given UserTemplate resource and its description
   with the server.  The resource document (resource) and the description
   (desc) must be valid.  The key will be used to create the id of
   the resource as 'user-template/key'."
  [resource desc]
  (when-let [{:keys [id] :as full-resource} (complete-resource resource)]
    (swap! templates assoc id full-resource)
    (log/info "loaded UserTemplate" id)
    (when desc
      (let [acl (:acl full-resource)
            full-desc (assoc desc :acl acl)]
        (swap! descriptions assoc id full-desc))
      (log/info "loaded UserTemplate description" id))))

;;
;; schemas
;;

(def UserTemplateDescription
  (merge c/CommonParameterDescription
         {:method {:displayName "Registration Method"
                   :category    "general"
                   :description "method to be used to register user"
                   :type        "string"
                   :mandatory   true
                   :readOnly    true
                   :order       0}}))
;;
;; multimethods for validation
;;

(defmulti validate-subtype
          "Validates the given resource against the specific
           UserTemplate subtype schema."
          :method)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown UserTemplate type: " (:method resource)) resource)))

(defmethod crud/validate
  resource-uri
  [resource]
  (validate-subtype resource))

;;
;; CRUD operations
;;

(defmethod crud/add resource-name
  [request]
  (throw (r/ex-bad-method request)))

(defmethod crud/retrieve resource-name
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (get @templates id)
          (a/can-view? request)
          (r/json-response)))
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
  (throw (r/ex-bad-method request)))

(defmethod crud/delete resource-name
  [request]
  (throw (r/ex-bad-method request)))

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
        options (select-keys request [:identity :query-params :cimi-params])
        count-before-pagination (count entries)
        wrapped-entries (wrapper-fn request entries)
        entries-and-count (assoc wrapped-entries :count count-before-pagination)]
    (r/json-response entries-and-count)))

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


