(ns com.sixsq.slipstream.ssclj.resources.common.std-crud
  "Standard CRUD functions for resources."
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [clojure.walk :as w]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.util.response :as r]))

(defn add-fn
  [resource-name collection-acl resource-uri]
  (fn [{:keys [body] :as request}]
    (a/can-modify? {:acl collection-acl} request)
    (db/add
      resource-name
      (-> body
          u/strip-service-attrs
          (crud/new-identifier resource-name)
          (assoc :resourceURI resource-uri)
          u/update-timestamps
          (crud/add-acl request)
          crud/validate)
      {})))

(defn retrieve-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (try
      (-> (str (u/de-camelcase resource-name) "/" uuid)
          (db/retrieve request)
          (a/can-view? request)
          (crud/set-operations request)
          (r/json-response))
      (catch Exception e
        (or (ex-data e) (throw e))))))

(defn edit-fn
  [resource-name]
  (fn [{{uuid :uuid} :params body :body :as request}]
    (try
      (let [current (-> (str (u/de-camelcase resource-name) "/" uuid)
                        (db/retrieve request)
                        (a/can-modify? request))
            merged (merge current body)]
        (-> merged
            (u/update-timestamps)
            (crud/validate)
            (db/edit request)))
      (catch Exception e
        (or (ex-data e) (throw e))))))

(defn delete-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (try
      (-> (str (u/de-camelcase resource-name) "/" uuid)
          (db/retrieve request)
          (a/can-modify? request)
          (db/delete request))
      (catch Exception e
        (or (ex-data e) (throw e))))))

(defn collection-wrapper-fn
  [resource-name collection-acl collection-uri collection-key]
  (fn [request entries]
    (let [skeleton {:acl         collection-acl
                    :resourceURI collection-uri
                    :id          (u/de-camelcase resource-name)}
          entries-with-operations (map #(crud/set-operations % request) entries)]
      (-> skeleton
          (crud/set-operations request)
          (assoc collection-key entries-with-operations)))))

(defn query-fn
  [resource-name collection-acl collection-uri collection-key]
  (fn [request]
    (a/can-view? {:acl collection-acl} request)
    (let [wrapper-fn (collection-wrapper-fn resource-name collection-acl collection-uri collection-key)
          options (select-keys request [:identity :query-params :cimi-params :user-name :user-roles])
          [metadata entries] (db/query resource-name options)
          entries-and-count (merge metadata (wrapper-fn request entries))]
      (r/json-response entries-and-count))))

(def ^:const href-not-found-msg "requested href not found")

(def ^:const href-not-accessible-msg "requested href cannot be accessed")

(defn resolve-href-keep
  "Pulls in the resource identified by the value of the :href key and merges
   that resource with argument. Keys specified directly in the argument take
   precedence. Common attributes in the referenced resource are stripped. If
   :href doesn't exist the argument is returned unchanged.

   If a referenced document doesn't exist or if the user doesn't have read
   access to the document, then the method will throw."
  [{:keys [href] :as resource} idmap]
  (if-not (str/blank? href)
    (if-let [refdoc (crud/retrieve-by-id href)]
      (try
        (a/can-view? refdoc idmap)
        (-> refdoc
            (u/strip-common-attrs)
            (u/strip-service-attrs)
            (dissoc :acl)
            (merge resource))
        (catch Exception ex
          (throw (r/ex-bad-request (format "%s: %s" href-not-accessible-msg href)))))
      (throw (r/ex-bad-request (format "%s: %s" href-not-found-msg href))))
    resource))

(defn resolve-href
  "Like resolve-href-keep, except that the :href attributes are removed."
  [{:keys [href] :as resource} idmap]
  (if href
    (-> resource
        (resolve-href-keep idmap)
        (dissoc :href))
    resource))

(defn resolve-hrefs
  "Does a prewalk of the given argument, replacing any map with an :href
   attribute with the result of merging the referenced resource (see the
   resolve-href function)."
  [resource idmap & [keep?]]
  (let [f (if keep? resolve-href-keep resolve-href)]
    (w/prewalk #(f % idmap) resource)))


(defn initialize
  "Perform the initialization of the database for a given resource type. If an
   exception is thrown, it will be logged but then ignored."
  [resource-url spec]
  (try
    (db/initialize resource-url {:spec spec})
    (catch Exception e
      (log/errorf "exception when initializing database for %s: %s"
                  resource-url (.getMessage e)))))
