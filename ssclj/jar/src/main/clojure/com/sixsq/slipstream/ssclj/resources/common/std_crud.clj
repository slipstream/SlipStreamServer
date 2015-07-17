(ns com.sixsq.slipstream.ssclj.resources.common.std-crud
  "Standard CRUD functions for resources."
  (:require
    [clojure.walk                                             :as w]
    [clojure.pprint                                           :refer [pprint]]
    [clojure.tools.logging                                    :as log]

    [com.sixsq.slipstream.ssclj.resources.common.authz        :as a]
    [com.sixsq.slipstream.ssclj.resources.common.utils        :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud         :as crud]
    [com.sixsq.slipstream.ssclj.db.impl                       :as db]

    [com.sixsq.slipstream.ssclj.resources.common.cimi-filter  :as cf]
    [com.sixsq.slipstream.ssclj.resources.common.pagination   :as pg]))

(defn log-request
  [request]
  (log/info (:request-method  request)
            (:uri             request)
            (:query-string    request)
            (:body            request)))

(defn add-fn
  [resource-name collection-acl resource-uri]
  (fn [{:keys [body] :as request}]
    (log-request request)
    (a/can-modify? {:acl collection-acl} request)
    (->> (->  body              
              u/strip-service-attrs              
              (crud/new-identifier resource-name)              
              (assoc :resourceURI resource-uri)              
              u/update-timestamps              
              (crud/add-acl request)                            
              crud/validate)
         (db/add resource-name))))

(defn retrieve-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (log-request request)
    (-> (str resource-name "/" uuid)
        db/retrieve
        (a/can-view? request)
        (crud/set-operations request)
        (u/json-response))))

(defn edit-fn
  [resource-name]
  (fn [{{uuid :uuid} :params body :body :as request}]
    (log-request request)
    (let [current (-> (str resource-name "/" uuid)
                      (db/retrieve)
                      (a/can-modify? request))]
      (->> body
           (u/strip-service-attrs)
           (merge current)
           (u/update-timestamps)
           (crud/validate)
           (db/edit)
           (u/json-response)))))

(defn delete-fn
  [resource-name]
  (fn [{{uuid :uuid} :params :as request}]
    (log-request request)
    (-> (str resource-name "/" uuid)
        (db/retrieve)
        (a/can-modify? request)
        (db/delete))))

(defn collection-wrapper-fn
  [resource-name collection-acl collection-uri collection-key]
  (let [skeleton (-> {:acl         collection-acl
                      :resourceURI collection-uri
                      :id          resource-name
                      :count       0})]
    (fn [request entries]
      (let [count (count entries)]
        (if (zero? count)
          skeleton
          (-> skeleton
              (crud/set-operations request)
              (assoc :count count)
              (assoc collection-key entries)))))))

(defn query-fn
  [resource-name collection-acl collection-uri collection-key]
  (let [wrapper-fn (collection-wrapper-fn resource-name collection-acl collection-uri collection-key)]
    (fn [request]
      (log-request request)
      (a/can-view? {:acl collection-acl} request)

      (->> (select-keys request [:identity :query-params :cimi-params])

           (db/query resource-name)

           u/walk-clojurify

           ;; filtering
           (cf/cimi-filter-tree (get-in request [:cimi-params :filter]))

           ;; ordering
           (crud/sort-collection request)

           ;; paginating
           (pg/paginate         (get-in request [:cimi-params :first]) (get-in request [:cimi-params :last]))

           ;; access controlling
           (filter #(a/authorized-view? % request))

           (map #(crud/set-operations % request))

           (wrapper-fn request)

           (u/json-response)))))

(defn resolve-href
  "Pulls in the resource identified by the value of the :href key
   and merges that resource with argument.  Keys specified directly
   in the argument take precedence.  Common attributes in the referenced
   resource are stripped. If :href doesn't exist the argument is
   returned unchanged."
  [{:keys [href] :as resource}]
  (if href
    (-> (db/retrieve href)
        (u/strip-common-attrs)
        (merge resource)
        (dissoc :href))
    resource))

(defn resolve-hrefs
  "Does a prewalk of the given argument, replacing any map with an :href
   attribute with the result of merging the referenced resource (see the
   resolve-href function)."
  [resource]
  (w/prewalk resolve-href resource))
