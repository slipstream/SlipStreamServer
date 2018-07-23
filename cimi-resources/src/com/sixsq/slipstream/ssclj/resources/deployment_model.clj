(ns com.sixsq.slipstream.ssclj.resources.deployment-model
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.module :as m]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-model :as dm]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-model-template :as dmt]))

(def ^:const resource-tag :deploymentModels)

(def ^:const resource-name "DeploymentModel")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "DeploymentModelCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; validate deployment models
;;

(def validate-fn (u/create-spec-validation-fn ::dm/deployment-model))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))


;;
;; validate create requests deployment model
;;

(def create-validate-fn (u/create-spec-validation-fn ::dmt/deployment-model-template-create))
(defmethod crud/validate create-uri
  [resource]
  (create-validate-fn resource))


;;
;; multimethod for ACLs
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))


;;
;; convert template to model
;;

;; FIXME: Add real implementation.
(defn tpl->model
  [{:keys [resourceURI module] :as resource} request]
  (let [model {:resourceURI resourceURI
               :module      module
               :nodes       [{:nodeID     "my-node-uuid"
                              :credential {:href "my-cred-uuid"}
                              :cpu        10
                              :ram        20
                              :disk       30}
                             {:nodeID     "my-second-node-uuid"
                              :credential {:href "my-second-cred-uuid"}
                              :cpu        100
                              :ram        200
                              :disk       300}]}]
    model))


;;
;; CRUD operations
;;

(defn resolve-hrefs
  [body idmap]
  (let [module-href (get-in body [:deploymentModelTemplate :module :href])
        request-module {:params   {:uuid          (some-> module-href (str/split #"/") second)
                                   :resource-name m/resource-url}
                        :identity idmap #_std-crud/internal-identity}
        module (some-> request-module
                       crud/retrieve
                       :body
                       (dissoc :versions :operations :acl))]
    (-> body
        (assoc-in [:deploymentModelTemplate :module] module)
        (std-crud/resolve-hrefs idmap)
        (assoc-in [:deploymentModelTemplate :module :href] module-href))))


(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))


(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        desc-attrs (u/select-desc-keys body)
        body (-> body
                 (assoc :resourceURI create-uri)
                 (resolve-hrefs idmap)
                 (update-in [:deploymentModelTemplate] merge desc-attrs) ;; ensure desc attrs are validated
                 crud/validate
                 :deploymentModelTemplate
                 (tpl->model request))]
    (add-impl (assoc request :body body))))


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
;; actions may be needed by certain authentication methods (notably external
;; methods like GitHub and OpenID Connect) to validate a given session
;;

(defmethod crud/do-action [resource-url "start"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)
          model (crud/retrieve-by-id-as-admin id)]
      (log/info (with-out-str (clojure.pprint/pprint model)))) ;; FIXME: Do something!
    (catch Exception e
      (or (ex-data e) (throw e)))))


;;
;; initialization: no schema for this parent resource
;;
(defn initialize
  []
  (std-crud/initialize resource-url nil))
