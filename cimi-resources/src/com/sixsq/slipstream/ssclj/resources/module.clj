(ns com.sixsq.slipstream.ssclj.resources.module
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.module :as module]
    [com.sixsq.slipstream.ssclj.resources.module-image :as module-image]
    [com.sixsq.slipstream.ssclj.resources.module-component :as module-component]
    [com.sixsq.slipstream.ssclj.resources.module-application :as module-application]
    [superstring.core :as str]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.util.response :as r]))

(def ^:const resource-name "Module")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ModuleCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "ALL"}]})

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn ::module/module))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

;;
;; use default ACL method
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(defn split-resource
  [{:keys [content] :as body}]
  (let [module-meta (dissoc body :content)]
    [module-meta content]))


(defn type->resource-name
  [type]
  (case type
    "IMAGE" module-image/resource-url
    "COMPONENT" module-component/resource-url
    "APPLICATION" module-application/resource-url
    (throw (r/ex-bad-request (str "unknown module type: " type)))))


(defn type->resource-uri
  [type]
  (case type
    "IMAGE" module-image/resource-uri
    "COMPONENT" module-component/resource-uri
    "APPLICATION" module-application/resource-uri
    (throw (r/ex-bad-request (str "unknown module type: " type)))))


(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (let [[{:keys [type] :as module-meta} module-content] (-> body u/strip-service-attrs split-resource)
        content-url (type->resource-name type)
        content-uri (type->resource-uri type)

        content-body (merge module-content {:resourceURI content-uri})
        content-request {:params   {:resource-name content-url}
                         :identity std-crud/internal-identity
                         :body     content-body}

        response (crud/add content-request)

        content-id (-> response :body :resource-id)
        module-meta (assoc module-meta :versions [{:href content-id}])]

    (db/add
      resource-name
      (-> module-meta
          u/strip-service-attrs
          (crud/new-identifier resource-name)
          (assoc :resourceURI resource-uri)
          u/update-timestamps
          (crud/add-acl request)
          crud/validate)
      {})))


(defn retrieve-edn
  [{{uuid :uuid} :params :as request}]
  (-> (str (u/de-camelcase resource-name) "/" uuid)
      (db/retrieve request)
      (a/can-view? request)))


(defmethod crud/retrieve resource-name
  [request]
  (try
    (let [{:keys [versions] :as module-meta} (retrieve-edn request)
          module-content (crud/retrieve-by-id-as-admin (-> versions last :href))]
      (-> (assoc module-meta :content module-content)
          (crud/set-operations request)
          (r/json-response)))
    (catch Exception e
      (or (ex-data e) (throw e)))))


(def edit-impl (std-crud/edit-fn resource-name))


(defmethod crud/edit resource-name
  [{:keys [body] :as request}]
  (try
    (let [id (str resource-url "/" (-> request :params :uuid))
          [module-meta module-content] (-> body u/strip-service-attrs split-resource)
          {:keys [type versions acl]} (crud/retrieve-by-id-as-admin id)

          _ (a/can-modify? {:acl acl} request)

          content-url (type->resource-name type)
          content-uri (type->resource-uri type)

          content-body (merge module-content {:resourceURI content-uri})
          content-request {:params   {:resource-name content-url}
                           :identity std-crud/internal-identity
                           :body     content-body}

          response (crud/add content-request)

          content-id (-> response :body :resource-id)

          versions (conj versions {:href content-id})
          module-meta (assoc module-meta :versions versions
                                         :type type)]

      (edit-impl (assoc request :body module-meta)))
    (catch Exception e
      (or (ex-data e) (throw e)))))


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
  (std-crud/initialize resource-url ::module/module))
