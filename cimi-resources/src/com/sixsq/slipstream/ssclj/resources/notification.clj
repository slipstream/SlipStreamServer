(ns com.sixsq.slipstream.ssclj.resources.notification
  "
Notification resources provide a timestamp for the occurrence of some action. These
are used within the SlipStream server to mark changes in the lifecycle of a
cloud application and for other important actions.

  "
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.notification.utils :as notification-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.notification :as notification]))

(def ^:const resource-tag :notifications)
(def ^:const resource-name notification-utils/resource-name)
(def ^:const resource-url notification-utils/resource-url)
(def ^:const collection-name "NotificationCollection")

(def ^:const resource-uri notification-utils/resource-uri)
(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl notification-utils/collection-acl)

;;
;; "Implementations" of multimethod declared in crud namespace
;;

(def validate-fn (u/create-spec-validation-fn ::notification/notification))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))


(defmethod crud/add resource-name
  [request]
  (notification-utils/add-impl request))


(def retrieve-impl (std-crud/retrieve-fn resource-name))

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
  [{{:keys [orderby]} :cimi-params :as request}]
  (query-impl (assoc-in request [:cimi-params :orderby] (if (seq orderby) orderby [["timestamp" :desc]]))))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::notification/notification))
