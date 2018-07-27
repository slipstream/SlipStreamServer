(ns com.sixsq.slipstream.ssclj.resources.deployment-template
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template :as dt]
    [com.sixsq.slipstream.util.response :as r]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.module :as m]))

(def ^:const resource-tag :deploymentTemplates)

(def ^:const resource-name "DeploymentTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "DeploymentTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

;; the templates are managed as in-memory resources, so modification
;; of the collection is not permitted, but users must be able to list
;; and view templates
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; description
;;


(def ^:const desc
  (merge c/CommonParameterDescription
         {:module {:displayName "Module"
                   :type        "string"
                   :description "module from which to create a deployment"
                   :mandatory   true
                   :readOnly    false
                   :order       20}}))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::dt/deployment-template))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))


;;
;; multimethod for ACLs
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))


;;
;; CRUD operations
;;


(defn resolve-hrefs
  [deployment-template idmap]
  (let [module-href (get-in deployment-template [:module :href])
        request-module {:params   {:uuid          (some-> module-href (str/split #"/") second)
                                   :resource-name m/resource-url}
                        :identity idmap}
        {:keys [body status] :as module-response} (some-> request-module
                                                          crud/retrieve)]

    (if (= status 200)
      (let [module (dissoc body :versions :operations :acl)]
        (-> deployment-template
            (assoc :module module)
            (std-crud/resolve-hrefs idmap)
            (assoc-in [:module :href] module-href)))
      (ex-info nil body))))


(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap (:identity request)
        resolved-body (resolve-hrefs body idmap)]
    (add-impl (assoc request :body resolved-body))))


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
;; initialization
;;


(defn initialize
  []
  (std-crud/initialize resource-url ::dt/deploymentTemplate))

;;
;; actions
;;


(defmethod crud/do-action [resource-url "describe"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)
          resource (crud/retrieve-by-id-as-admin id)]
      (a/can-view? resource request)
      (r/json-response desc))
    (catch Exception e
      (or (ex-data e) (throw e)))))
