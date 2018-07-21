(ns com.sixsq.slipstream.ssclj.resources.deployment-model-template
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-model-template :as dmt]
    [com.sixsq.slipstream.util.response :as r]))

(def ^:const resource-tag :deploymentModelTemplates)

(def ^:const resource-name "DeploymentModelTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "DeploymentModelTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

;; the templates are managed as in-memory resources, so modification
;; of the collection is not permitted, but users must be able to list
;; and view templates
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(def resource-acl collection-acl)


;;
;; resource
;;

(def ^:const resource
  {:name        "Deployment Model Template"
   :description "Deployment Model Template"
   :module      {:href "module/my-deployment-module"}})


;;
;; description
;;

(def ^:const desc
  (merge c/CommonParameterDescription
         {:module {:displayName "Module"
                   :type        "string"
                   :description "module from which to create a deployment model"
                   :mandatory   true
                   :readOnly    false
                   :order       20}}))


;;
;; atom to keep track of the loaded DeploymentModelTemplate resources
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
  "Completes the given document with server-managed information: resourceURI,
   timestamps, and operations. NOTE: The subtype MUST provide an ACL for the
   template."
  [resource]
  (let [id (str resource-url "/standard")
        href (str id "/describe")
        ops [{:rel (:describe c/action-uri) :href href}]]
    (-> resource
        (merge {:id          id
                :resourceURI resource-uri
                :operations  ops
                :acl         resource-acl})
        u/update-timestamps)))

(defn register
  "Registers a given DeploymentModelTemplate resource and its description with the
   server. The resource document (resource) and the description (desc) must be
   valid. The template-id key must be provided; it will be used to generate the
   id of the form 'credential-template/template-id'."
  [resource desc]
  (when-let [{:keys [id] :as full-resource} (complete-resource resource)]
    (swap! templates assoc id full-resource)
    (log/info "loaded DeploymentModelTemplate" id)
    (when desc
      (let [acl (:acl full-resource)
            full-desc (assoc desc :acl acl)]
        (swap! descriptions assoc id full-desc))
      (log/info "loaded DeploymentModelTemplate description" id))))

;;
;; schemas
;;

(def DeploymentModelTemplateDescription
  (merge c/CommonParameterDescription
         {:type   {:displayName "Credential Type"
                   :category    "general"
                   :description "type of credential"
                   :type        "string"
                   :mandatory   true
                   :readOnly    true
                   :order       10}
          :method {:displayName "Credential Creation Method"
                   :category    "general"
                   :description "method for creating credential"
                   :type        "string"
                   :mandatory   true
                   :readOnly    true
                   :order       11}}))
;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::dmt/deployment-model-template))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))


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
    (catch Exception e
      (or (ex-data e) (throw e)))))


;; must override the default implementation so that the
;; data can be pulled from the atom rather than the database
(defmethod crud/retrieve-by-id resource-url
  [id]
  (try
    (get @templates id)
    (catch Exception e
      (or (ex-data e) (throw e)))))


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
        options (select-keys request [:identity :query-params :cimi-params :credential-name :credential-roles])
        count-before-pagination (count entries)
        wrapped-entries (wrapper-fn request entries)
        entries-and-count (assoc wrapped-entries :count count-before-pagination)]
    (r/json-response entries-and-count)))


;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (register resource desc))


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


