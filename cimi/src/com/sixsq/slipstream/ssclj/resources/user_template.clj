(ns com.sixsq.slipstream.ssclj.resources.user-template
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template]
    [com.sixsq.slipstream.util.response :as r]))

(def ^:const resource-tag :userTemplates)

(def ^:const resource-name "UserTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "UserTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

;; anonymous users must be able to list and view templates (if anonymous
;; registration is permitted)
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; default ACL for description resources (these are ephemeral resources)
;;
(def description-acl {:owner {:principal "ADMIN"
                              :type      "ROLE"}
                      :rules [{:principal "ANON"
                               :type      "ROLE"
                               :right     "VIEW"}
                              {:principal "USER"
                               :type      "ROLE"
                               :right     "VIEW"}]})

;;
;; atom to keep track of the UserTemplate descriptions
;;
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


(defn register
  "Registers a given UserTemplate resource and its description
   with the server.  The resource document (resource) and the description
   (desc) must be valid.  The key will be used to create the id of
   the resource as 'user-template/key'."
  [id desc]
  (when (and id desc)
    (let [full-desc (assoc desc :acl description-acl)]
      (swap! descriptions assoc id full-desc))
    (log/info "loaded UserTemplate description" id)))

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
                   :order       10}}))
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

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [{{:keys [method]} :body :as request}]
  (if (get @descriptions method)
    (add-impl request)
    (throw (r/ex-bad-request (str "invalid user registration method '" method "'")))))

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
;; actions
;;
(defmethod crud/do-action [resource-url "describe"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (-> (get @descriptions id)
          (a/can-view? request)
          (r/json-response)))
    (catch Exception e
      (or (ex-data e) (throw e)))))
