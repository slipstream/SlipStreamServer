(ns
  com.sixsq.slipstream.ssclj.resources.event
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]

    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(def ^:const resource-tag :events)
(def ^:const resource-name "Event")
(def ^:const resource-url (u/de-camelcase resource-name))
(def ^:const collection-name "EventCollection")

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))
(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "ALL"}]})
;;
;; schemas
;;

(def ^:private severity-levels (s/enum "critical" "high" "medium" "low"))
(def ^:private event-types (s/enum "state" "alarm" "action" "system"))

(def ^:private EventContent
  {:resource c/ResourceLink
   :state    s/Str})

(def Event
  (merge
    c/CreateAttrs
    c/AclAttr
    {
     :id        c/NonBlankString
     :timestamp c/Timestamp
     :content   EventContent
     :type      event-types
     :severity  severity-levels}))

;;
;; "Implementations" of multimethod declared in crud namespace
;;

(def validate-fn (u/create-validation-fn Event))

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
  (add-impl request))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

;;
;; single
;;
(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

;;
;; available operations
;;
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
;; collection
;;

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))
