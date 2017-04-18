(ns com.sixsq.slipstream.ssclj.resources.session-template
  (:require
    [clojure.tools.logging :as log]
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-tag :sessionTemplates)

(def ^:const resource-name "SessionTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "SessionTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ANON"
                            :type      "ROLE"
                            :right     "VIEW"}
                           {:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}]})

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; atom to keep track of the loaded SessionTemplate resources
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
  [{:keys [method] :as resource}]
  (when method
    (let [id (str resource-url "/" method)
          href (str id "/describe")
          ops [{:rel (:describe c/action-uri) :href href}]]
      (-> resource
          (merge {:id          id
                  :resourceURI resource-uri
                  :acl         resource-acl
                  :operations  ops})
          u/update-timestamps))))

(defn register
  "Registers a given SessionTemplate resource and its description
   with the server.  The resource document (resource) and the description
   (desc) must be valid.  The key will be used to create the id of
   the resource as 'session-template/key'."
  [resource desc]
  (when-let [{:keys [id] :as full-resource} (complete-resource resource)]
    (swap! templates assoc id full-resource)
    (log/info "loaded SessionTemplate" id)
    (when desc
      (let [acl (:acl full-resource)
            full-desc (assoc desc :acl acl)]
        (swap! descriptions assoc id full-desc))
      (log/info "loaded SessionTemplate description" id))))

;;
;; schemas
;;

(def SessionTemplateDescription
  (merge c/CommonParameterDescription
         {:method {:displayName "Authentication Method"
                   :category    "general"
                   :description "method to be used to authenticate user"
                   :type        "string"
                   :mandatory   true
                   :readOnly    true
                   :order       0}}))
;;
;; multimethods for validation
;;

(defmulti validate-subtype
          "Validates the given resource against the specific
           SessionTemplate subtype schema."
          :method)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown SessionTemplate type: " (:method resource)) resource)))

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


