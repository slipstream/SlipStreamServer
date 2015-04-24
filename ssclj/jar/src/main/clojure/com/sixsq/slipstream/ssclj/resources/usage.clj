(ns 
  com.sixsq.slipstream.ssclj.resources.usage
  (:require
    [clojure.tools.logging                                      :as log]
    [schema.core                                                :as s]    
    [korma.core                                                 :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.db.database-binding             :as dbb]
    [com.sixsq.slipstream.ssclj.database.ddl                    :as ddl]
    [com.sixsq.slipstream.ssclj.resources.common.authz          :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud           :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud       :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils          :as u]
    [com.sixsq.slipstream.ssclj.db.filesystem-binding-utils     :as fu]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]
    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]))

(def ^:const resource-tag     :usages)
(def ^:const resource-name    "Usage")
(def ^:const collection-name  "UsageCollection")

(def ^:const resource-uri     (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri   (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal   "ADMIN"
                             :type        "ROLE"}

                     :rules [{:principal  "ANON"
                              :type       "ROLE"
                              :right      "VIEW"}]})

(defonce init-record-keeper (rc/-init))

(defentity usage-summaries)
(defentity acl)

(defn- deserialize-usage   
  [usage]
  (update-in usage [:acl] fu/deserialize))

(defn id-roles   
  [options]
  (-> options
      :authentications 
      (get (:current options))
      ((juxt :identity :roles))))

; (defn roles-in   
;   [roles]
;   (str     
;     "(" 
;       (ddl/double-quote "acl") "." 
;       (ddl/double-quote "principal-name")
;     " IN "
;     (ddl/simple-quote-list roles)) ")")


 (defmethod dbb/find-resources resource-name
  [collection-id options]
  (->> (select usage-summaries (order :start_timestamp :DESC))
       (map deserialize-usage)))

; (defmethod dbb/find-resources resource-name
;   [collection-id options]
;   (let [[id roles] (id-roles options)] 
;     (->>  
;         (select usage-summaries
;             (modifier "DISTINCT")
;             (join :inner acl
;               (or 
;                 (and  (= :acl.principal-type "USER")
;                       (= :acl.principal-name id))))
;                 ; (and  (= :acl.principal-type "ROLE")
;                 ;       (raw (roles-in roles)))))
;             (order :start_timestamp :DESC))

;         (map deserialize-usage))))           

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

;;
;; single
;;

(defn- check-exist 
  [resource id]
  (if (empty? resource)
    (throw (u/ex-not-found id))
    resource))

(defn find-resource
  [id]
  (-> (select usage-summaries (where {:id id}) (limit 1))
      (check-exist id)
      first            
      deserialize-usage))

(defn retrieve-fn
  [request]
  (fn [{{uuid :uuid} :params :as request}]
    (-> (str resource-name "/" uuid)        
        find-resource        
        (a/can-view? request)
        (crud/set-operations request)
        (u/json-response))))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-fn request))
