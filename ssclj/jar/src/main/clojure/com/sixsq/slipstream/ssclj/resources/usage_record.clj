(ns com.sixsq.slipstream.ssclj.resources.usage-record
  (:refer-clojure                                           :exclude [update])
  (:require
    [clojure.tools.logging                                  :as log]
    [schema.core                                            :as s]
    [com.sixsq.slipstream.ssclj.resources.common.crud       :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud   :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils      :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema     :as c]))

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
     :id                              c/NonBlankString
     :cloud_vm_instanceid             c/NonBlankString
     :user                            c/NonBlankString
     :cloud                           c/NonBlankString
     :start_timestamp                 c/Timestamp
     (s/optional-key :end_timestamp)  c/OptionalTimestamp
     :metric_name                     c/NonBlankString
     :metric_value                    c/Numeric}))

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
  (log/info resource-uri ": will add usage record" (:body request))
  (add-impl request))

;;
;; collection
;;
(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))