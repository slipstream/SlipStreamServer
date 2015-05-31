(ns com.sixsq.slipstream.ssclj.resources.usage-record  
  (:require
    [clojure.tools.logging                                  :as log]
    [schema.core                                            :as s]
    [com.sixsq.slipstream.ssclj.db.database-binding         :as dbb]
    [com.sixsq.slipstream.ssclj.resources.common.authz      :as a]  
    [com.sixsq.slipstream.ssclj.resources.common.crud       :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud   :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils      :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema     :as c]
    [com.sixsq.slipstream.ssclj.usage.record-keeper         :as rc]))

(def ^:const resource-tag     :usage-records)
(def ^:const resource-name    "UsageRecord")
(def ^:const collection-name  "UsageRecordCollection")

(def ^:const resource-uri   (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "ALL"}]})

(def UsageRecord
  (merge
    c/CreateAttrs
    c/AclAttr
    {
     :id                                c/NonBlankString
     :cloud_vm_instanceid               c/NonBlankString
     :user                              c/NonBlankString
     :cloud                             c/NonBlankString
     (s/optional-key :start_timestamp)  c/Timestamp
     (s/optional-key :end_timestamp)    c/OptionalTimestamp
     (s/optional-key :metrics)          [{:name   c/NonBlankString
                                          :value  c/NonBlankString}]}))

(defmethod dbb/store-in-db resource-name
  [collection-id id data]
  (let [data-stripped (select-keys data [:cloud_vm_instanceid :user :cloud :start_timestamp :end_timestamp :metrics])]
    (if (nil? (:end_timestamp data))
      (rc/-insertStart  data-stripped)
      (rc/-insertEnd    data-stripped))))

;;
;; "Implementations" of multimethod declared in crud namespace
;;

(def validate-fn (u/create-validation-fn UsageRecord))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

;;
;; Create
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (log/info resource-uri ": Will add usage record" (:body request))
  (add-impl request))
  ; (log/info resource-uri ": Done adding usage record" (:body request)))

(defmethod crud/set-operations resource-uri
  [resource request]
  (try
    (a/can-modify? resource request)
    (let [href (:id resource)
          ^String resourceURI (:resourceURI resource)
          ops (if (.endsWith resourceURI "Collection")
                [{:rel (:add c/action-uri) :href href}]
                [{:rel (:delete c/action-uri) :href href}])]
      (assoc resource :operations ops))
    (catch Exception e
      (dissoc resource :operations))))

;;
;; collection
;;
(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))