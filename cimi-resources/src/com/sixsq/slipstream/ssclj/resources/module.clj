(ns com.sixsq.slipstream.ssclj.resources.module
  (:require
    [clojure.string :as s]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.module.utils :as module-utils]
    [com.sixsq.slipstream.ssclj.resources.module-application :as module-application]
    [com.sixsq.slipstream.ssclj.resources.module-component :as module-component]
    [com.sixsq.slipstream.ssclj.resources.module-image :as module-image]
    [com.sixsq.slipstream.ssclj.resources.spec.module :as module]
    [com.sixsq.slipstream.util.response :as r]
    [superstring.core :as str]))

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
  (let [[{:keys [type] :as module-meta} module-content] (-> body u/strip-service-attrs module-utils/split-resource)]
    (if (= "PROJECT" type)
      (let [module-meta (module-utils/set-parent-path module-meta)]

        (db/add
          resource-name
          (-> module-meta
              u/strip-service-attrs
              (crud/new-identifier resource-name)
              (assoc :resourceURI resource-uri)
              u/update-timestamps
              (crud/add-acl request)
              crud/validate)
          {}))
      (let [content-url (type->resource-name type)
            content-uri (type->resource-uri type)

            content-body (merge module-content {:resourceURI content-uri})

            content-request {:params   {:resource-name content-url}
                             :identity std-crud/internal-identity
                             :body     content-body}

            response (crud/add content-request)

            content-id (-> response :body :resource-id)
            module-meta (-> (assoc module-meta :versions [{:href content-id}])
                            module-utils/set-parent-path)]

        (db/add
          resource-name
          (-> module-meta
              u/strip-service-attrs
              (crud/new-identifier resource-name)
              (assoc :resourceURI resource-uri)
              u/update-timestamps
              (crud/add-acl request)
              crud/validate)
          {})))))

(defn split-uuid
  [uuid]
  (let [[uuid-module index] (s/split uuid #"_")
        index (some-> index read-string)]
    [uuid-module index]))

(defn retrieve-edn
  [{{uuid :uuid} :params :as request}]
  (-> (str (u/de-camelcase resource-name) "/" (-> uuid split-uuid first))
      (db/retrieve request)
      (a/can-view? request)))


(defn retrieve-content-id
  [versions index]
  (if index
    (-> versions (nth index) :href)
    (->> versions (remove nil?) last :href)))


(defmethod crud/retrieve resource-name
  [{{uuid :uuid} :params :as request}]
  (try
    (let [{:keys [versions] :as module-meta} (retrieve-edn request)
          version-index (second (split-uuid uuid))
          version-id (retrieve-content-id versions version-index)
          module-content (if version-id
                           (crud/retrieve-by-id-as-admin version-id)
                           (when version-index
                             (throw (r/ex-not-found (str "Module version not found: " resource-url "/" uuid)))))]
      (-> (assoc module-meta :content module-content)
          (crud/set-operations request)
          (r/json-response)))
    (catch IndexOutOfBoundsException _
      (r/response-not-found (str resource-url "/" uuid)))
    (catch Exception e
      (or (ex-data e) (throw e)))))


(def edit-impl (std-crud/edit-fn resource-name))


(defmethod crud/edit resource-name
  [{:keys [body] :as request}]
  (try
    (let [id (str resource-url "/" (-> request :params :uuid))
          [module-meta module-content] (-> body u/strip-service-attrs module-utils/split-resource)
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
          module-meta (-> (assoc module-meta :versions versions
                                             :type type)
                          module-utils/set-parent-path)]

      (edit-impl (assoc request :body module-meta)))
    (catch Exception e
      (or (ex-data e) (throw e)))))

(defn remove-version
  [versions index]
  (let [part-a (subvec versions 0 index)
        part-b (subvec versions (inc index))]
    (concat part-a [nil] part-b)))


(def delete-impl (std-crud/delete-fn resource-name))

(defn delete-content
  [content-id type]
  (let [delete-request {:params   {:uuid          (-> content-id u/split-resource-id second)
                                   :resource-name (type->resource-name type)}
                        :identity std-crud/internal-identity
                        :body     {:id content-id}}]
    (crud/delete delete-request)))

(defn delete-all
  [request {:keys [type versions] :as module-meta}]
  (doseq [version versions]
    (when version
      (delete-content (:href version) type)))
  (delete-impl request))

(defn delete-item
  [request {:keys [type versions] :as module-meta} version-index]
  (let [content-id (retrieve-content-id versions version-index)
        delete-response (delete-content content-id type)
        updated-versions (remove-version versions version-index)
        module-meta (assoc module-meta :versions updated-versions)
        {:keys [status]} (edit-impl (assoc request :request-method :put
                                                   :body module-meta))]
    (when (not= status 200)
      (throw (r/ex-response "A failure happened during delete module item" 500)))

    delete-response))

(defmethod crud/delete resource-name
  [{{uuid-full :uuid} :params :as request}]
  (try

    (let [module-meta (retrieve-edn request)

          _ (a/can-modify? module-meta request)

          [uuid version-index] (split-uuid uuid-full)
          request (assoc-in request [:params :uuid] uuid)]

      (if version-index
        (delete-item request module-meta version-index)
        (delete-all request module-meta)))

    (catch IndexOutOfBoundsException _
      (r/response-not-found (str resource-url "/" uuid-full)))
    (catch Exception e
      (or (ex-data e) (throw e)))))


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
