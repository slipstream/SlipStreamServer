(ns com.sixsq.slipstream.ssclj.resources.deployment-model
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-model :as dm]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-model-template :as dmt]
    [com.sixsq.slipstream.ssclj.resources.module :as m]
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [com.sixsq.slipstream.auth.acl :as acl]))

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
  [{:keys [module] :as resource} request]
  (let [model {:module module
               :nodes  [{:nodeID     "my-node-uuid"
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

(defn add-impl [{:keys [id body] :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (db/add
    resource-name
    (-> body
        u/strip-service-attrs
        (assoc :id id)
        (assoc :resourceURI resource-uri)
        u/update-timestamps
        (crud/add-acl request)
        crud/validate)
    {}))


(defn resolve-hrefs
  [body idmap]
  (let [module-href (get-in body [:deploymentModelTemplate :module :href])
        request-module {:sixsq.slipstream.authn/claims (acl/current-authentication idmap)
                        :params                        {:uuid          (some-> module-href (str/split #"/") second)
                                                        :resource-name m/resource-url}
                        :route-params                  {:uuid          (some-> module-href (str/split #"/") second)
                                                        :resource-name m/resource-url}
                        :identity                      idmap
                        }
        _ (log/error idmap)
        module (some-> request-module
                       crud/retrieve
                       :body
                       (dissoc :versions))]
    (-> body
        (assoc-in [:deploymentModelTemplate :module] module)
        (std-crud/resolve-hrefs idmap)
        (assoc-in [:deploymentModelTemplate :module :href] module-href))))


(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap      {:identity (:identity request)}
        desc-attrs (u/select-desc-keys body)
        [create-resp {:keys [id] :as body}] (-> body
                                                (assoc :resourceURI create-uri)
                                                (resolve-hrefs idmap)
                                                (update-in [:deploymentModelTemplate] merge desc-attrs) ;; ensure desc attrs are validated
                                                crud/validate
                                                :deploymentModelTemplate
                                                (tpl->model request))]
    (-> request
        (assoc :id id :body (merge body desc-attrs))
        add-impl
        (update-in [:body] merge create-resp))))


(def retrieve-impl (std-crud/retrieve-fn resource-name))


(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))


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
