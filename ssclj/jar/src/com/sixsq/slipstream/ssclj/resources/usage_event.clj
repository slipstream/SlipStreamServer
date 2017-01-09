(ns com.sixsq.slipstream.ssclj.resources.usage-event
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.usage-event.spec :as spec]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rk]
    ))

(def ^:const resource-tag :usage-events)
(def ^:const resource-name "UsageEvent")
(def ^:const resource-url (u/de-camelcase resource-name))
(def ^:const collection-name "UsageEventCollection")

(def ^:const resource-uri   (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "ALL"}]})

;;
;; "Implementations" of multimethod declared in crud namespace
;;

(def validate-fn (u/create-spec-validation-fn ::spec/usage-event))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

;;
;; Create
;;


(defmethod crud/add resource-name
  [request]
  (rk/insert-usage-event (:body request) (select-keys request [:user-name :user-roles :cimi-params]))
  (u/map-response "Usage records created/updated" 201))


