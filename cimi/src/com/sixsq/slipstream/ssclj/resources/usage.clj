(ns
  com.sixsq.slipstream.ssclj.resources.usage
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.usage]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(def ^:const resource-tag :usages)
(def ^:const resource-name "Usage")
(def ^:const resource-url (u/de-camelcase resource-name))
(def ^:const collection-name "UsageCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}

                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}]})


(def validate-fn (u/create-spec-validation-fn :cimi/usage))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))
(defmethod crud/add resource-name
  [request]
  (add-impl request))

;;
;; collection
;;

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl (update-in request [:cimi-params] #(assoc % :orderby [["start-timestamp" :desc]]))))

;;
;; single
;;
(def retrieve-impl (std-crud/retrieve-fn resource-name))
(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

