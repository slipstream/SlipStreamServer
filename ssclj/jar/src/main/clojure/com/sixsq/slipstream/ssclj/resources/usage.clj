(ns 
  com.sixsq.slipstream.ssclj.resources.usage
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]    
    [korma.core :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbb]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.db.filesystem-binding-utils :as fu]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(def ^:const resource-tag :usages)
(def ^:const resource-name "Usage")
(def ^:const collection-name "UsageCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(defonce init-record-keeper (rc/-init))

(defentity usage-summaries)

(defmethod dbb/find-resources resource-name
  [collection-id]
  (->> (select usage-summaries)
    (map #(update-in % [:acl] fu/deserialize))))

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
       ;   c/NonBlankString { ;; metric-name
       ;     :cloud_vm_instanceid      c/NonBlankString
       ;     :unit_minutes   c/NonBlankString } 
       ; }
    }))

(def validate-fn (u/create-validation-fn Usage))

;;
;; collection
;;

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
           [request]
  (query-impl request))
