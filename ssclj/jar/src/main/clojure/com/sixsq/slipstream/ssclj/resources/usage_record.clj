(ns com.sixsq.slipstream.ssclj.resources.usage-record
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]))

(def ^:const resource-tag :usage-records)
(def ^:const resource-name "UsageRecord")
(def ^:const resource-url (cu/de-camelcase resource-name))
(def ^:const collection-name "UsageRecordCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "ALL"}]})

(def date-in-future "2100-01-01T00:00:00.000Z")

(def UsageRecord
  (merge
    c/CreateAttrs
    c/AclAttr
    {
     :id                                c/NonBlankString
     :cloud-vm-instanceid               c/NonBlankString
     :user                              c/NonBlankString
     :cloud                             c/NonBlankString
     (s/optional-key :start-timestamp)  c/Timestamp
     (s/optional-key :end-timestamp)    c/OptionalTimestamp
     :metric-name                       c/NonBlankString
     :metric-value                      c/NonBlankString}))

;;
;; "Implementations" of multimethod declared in crud namespace
;;

(def validate-fn (cu/create-validation-fn UsageRecord))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

;;
;; Add
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (add-impl request))

(defmethod crud/set-operations resource-uri
  [resource request]
  (try
    (a/can-modify? resource request)
    (let [href                (:id resource)
          ^String resourceURI (:resourceURI resource)
          ops                 (if (.endsWith resourceURI "Collection")
                                [{:rel (:add c/action-uri) :href href}]
                                [{:rel (:delete c/action-uri) :href href}])]
      (assoc resource :operations ops))
    (catch Exception e
      (dissoc resource :operations))))

;;
;; Query
;;
(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))

;;
;; edit
;;

(def edit-impl (std-crud/edit-fn resource-name))
(defmethod crud/edit resource-name
  [request]
  (println "crud/edit for usage-record")
  (edit-impl request))

;;
(defn last-record
  "Retrieves the most recent (start-timestamp) usage record with the same cloud-vm-instanceid and metric name."
  [usage-record]
  (let [filter
        (str "cloud-vm-instanceid='" (:cloud-vm-instanceid usage-record)
             "' and metric-name='" (:metric-name usage-record) "'")]
    (-> (db/query "usage-record" {:cimi-params {:filter  filter :orderby [["start-timestamp" :desc]]}
                                  :user-roles ["ADMIN"]})
        second
        first)))

(defn open-records
  "Retrieves open usage records with the same cloud-vm-instanceid and metric name."
  [usage-record]
  (let [filter
        (str "cloud-vm-instanceid='" (:cloud-vm-instanceid usage-record)
             "' and metric-name='" (:metric-name usage-record) "'"
             " and end-timestamp='" date-in-future "'")]
    (-> (db/query "usage-record" {:cimi-params {:filter filter}
                                  :user-roles ["ADMIN"]})
        second)))

(defn records-for-interval
  "Retrieves all usage records intersecting with given interval."
  [start end]
  (u/check-order [start end])
  (let [filter (str "end-timestamp >= '" start "' and start-timestamp <= '" end "'")]
    ;(println "filter = " filter)
    (-> (db/query "usage-record" {:cimi-params {:filter filter}
                                  :user-roles ["ADMIN"]})
        second)))
