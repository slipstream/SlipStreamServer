(ns
  com.sixsq.slipstream.ssclj.resources.usage
  (:refer-clojure :exclude [update])
  (:require

    [clojure.java.jdbc                                          :as j]
    [clojure.tools.logging                                      :as log]
    [korma.core                                                 :refer :all]
    [honeysql.core                                              :as sql]
    [honeysql.helpers                                           :as hh]

    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.database.korma-helper           :as kh]
    [com.sixsq.slipstream.ssclj.db.database-binding             :as dbb]
    [com.sixsq.slipstream.ssclj.resources.common.cimi-filter    :as cf]
    [com.sixsq.slipstream.ssclj.resources.common.authz          :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud           :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud       :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils          :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]
    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]))

(def ^:const resource-tag     :usages)
(def ^:const resource-name    "Usage")
(def ^:const resource-url (u/de-camelcase resource-name))
(def ^:const collection-name  "UsageCollection")

(def ^:const resource-uri     (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri   (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal   "ADMIN"
                             :type        "ROLE"}

                     :rules [{:principal  "ANON"
                              :type       "ROLE"
                              :right      "VIEW"}]})

(defonce init-record-keeper (rc/-init))

(defentity usage_summaries)
(defentity acl)
(database usage_summaries kh/korma-api-db)
(database acl kh/korma-api-db)

(defn- deserialize-usage
  [usage]
  (-> usage
      (update-in [:acl]   u/deserialize)
      (update-in [:usage] u/deserialize)))

(defn sql
  [id roles cimi-filter]
  (->   (hh/select :u.*)
        (hh/from    [:acl :a] [:usage_summaries :u])
        (hh/where   (u/into-vec-without-nil :and
                                        [
                                          [:= :u.id :a.resource-id]

                                          [:or
                                           (dbb/id-matches? id)
                                           (dbb/roles-in? roles)]

                                          (cf/sql-clauses cimi-filter)
                                         ]))

        (hh/modifiers :distinct)
        (hh/order-by [:u.start_timestamp :desc])

        (sql/format :quoting :ansi)))

(defmethod dbb/find-resources resource-name
  [collection-id options]
  (let [[id roles]  (dbb/id-roles options)]
    (if (dbb/neither-id-roles? id roles)
      []
      (do
        (->>  (sql id roles (get-in options [:cimi-params :filter]))
              (j/query kh/db-spec)
              (map deserialize-usage))))))

;;
;; schemas
;;

(def Usage
  (merge
    c/CreateAttrs
    c/AclAttr
    {
      :id               c/NonBlankString
      :user             c/NonBlankString
      :cloud            c/NonBlankString
      :start_timestamp  c/Timestamp
      :end_timestamp    c/Timestamp
      :usage            c/NonBlankString
    }))

(def validate-fn (u/create-validation-fn Usage))

;;
;; collection
;;

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))

;;
;; single
;;

(defn- check-presence
  [resources id]
  (if (empty? resources)
    (do
      (log/warn "Resource not found, id=" id)
      (throw (u/ex-not-found id)))
    resources))

(defn find-resource
  [id]
  (-> (select usage_summaries (where {:id id}) (limit 1))
      (check-presence id)
      first
      deserialize-usage))

(defn retrieve-fn
  [request]
  (fn [{{uuid :uuid} :params :as request}]
    (-> (str (u/de-camelcase resource-name) "/" uuid)
        find-resource
        (a/can-view? request)
        (crud/set-operations request)
        (u/json-response))))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-fn request))
