(ns
  com.sixsq.slipstream.ssclj.resources.event
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.event.utils :as event-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.event :as event]))

(def ^:const resource-tag :events)
(def ^:const resource-name event-utils/resource-name)
(def ^:const resource-url event-utils/resource-url)
(def ^:const collection-name "EventCollection")

(def ^:const resource-uri event-utils/resource-uri)
(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def collection-acl event-utils/collection-acl)

;;
;; "Implementations" of multimethod declared in crud namespace
;;

(def validate-fn (u/create-spec-validation-fn ::event/event))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))


(defmethod crud/add resource-name
  [request]
  (event-utils/add-impl request))


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
  (std-crud/initialize resource-url ::event/event))
