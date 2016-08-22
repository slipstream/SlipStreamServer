(ns com.sixsq.slipstream.ssclj.resources.common.std-crud
  "Standard CRUD functions for resources."
  (:require
    [clojure.walk :as w]
    [clojure.pprint :refer [pprint]]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.es.es-binding :as esb])
  (:import (clojure.lang ExceptionInfo)))

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
          (crud/set-operations request)
          (u/json-response))
      (catch ExceptionInfo ei
        (ex-data ei)))))

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
      (catch ExceptionInfo ei
        (ex-data ei)))))

(defn delete-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (try
      (-> (str (u/de-camelcase resource-name) "/" uuid)
          (db/retrieve request)
          (a/can-modify? request)
          (db/delete request))
      (catch ExceptionInfo ei
        (ex-data ei)))))

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
          [count-before-pagination entries] (db/query resource-name options)
          wrapped-entries (wrapper-fn request entries)
          entries-and-count (assoc wrapped-entries :count count-before-pagination)]
      (u/json-response entries-and-count))))


(defn resolve-href
  "Pulls in the resource identified by the value of the :href key
   and merges that resource with argument.  Keys specified directly
   in the argument take precedence.  Common attributes in the referenced
   resource are stripped. If :href doesn't exist the argument is
   returned unchanged.

   If a referenced document doesn't exist or if the user doesn't have
   read access to the document, then the method will throw."
  [{:keys [href] :as resource} idmap]
  (if href
    (let [refdoc (crud/retrieve-by-id href)]
      (a/can-view? refdoc idmap)
      (-> refdoc
          (u/strip-common-attrs)
          (u/strip-service-attrs)
          (dissoc :acl)
          (merge resource)
          (dissoc :href)))
    resource))

(defn resolve-hrefs
  "Does a prewalk of the given argument, replacing any map with an :href
   attribute with the result of merging the referenced resource (see the
   resolve-href function)."
  [resource idmap]
  (w/prewalk #(resolve-href % idmap) resource))
