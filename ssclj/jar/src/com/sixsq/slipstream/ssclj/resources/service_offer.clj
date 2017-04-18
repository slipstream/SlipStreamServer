(ns com.sixsq.slipstream.ssclj.resources.service-offer
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.service-offer]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as sn]
    [superstring.core :as str]
    [ring.util.response :as r]))

(def ^:const resource-name "ServiceOffer")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ServiceOfferCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; multimethods for validation and operations
;;

(defn valid-attribute-name?
  [valid-prefixes attr-name]
  (let [[ns _] (str/split (name attr-name) #":")]
    (valid-prefixes ns)))

(defn- valid-attributes?
  [validator resource]
  (if-not (map? resource)
    true
    (and (every? validator (keys resource))
         (every? (partial valid-attributes? validator) (vals resource)))))

(defn- throw-wrong-namespace
  []
  (let [code 406
        msg "resource attributes do not satisfy defined namespaces"
        response (-> {:status code :message msg}
                     u/json-response
                     (r/status code))]
    (throw (ex-info msg response))))

(defn- validate-attributes
  [resource]
  (let [valid-prefixes    (sn/all-prefixes)
        resource-payload  (dissoc resource :acl :id :resourceURI :name :description
                                 :created :updated :properties :operations :connector)
        validator         (partial valid-attribute-name? valid-prefixes)]
    (if (valid-attributes? validator resource-payload)
      resource
      (throw-wrong-namespace))))

(def validate-fn (u/create-spec-validation-fn :cimi/service-offer))
(defmethod crud/validate resource-uri
           [resource]
    (-> resource
        validate-fn
        validate-attributes))

(defmethod crud/add-acl resource-uri
           [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
           [request]
  (retrieve-impl request))

(def edit-impl (std-crud/edit-fn resource-name))

(defmethod crud/edit resource-name
           [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
           [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
           [request]
  (query-impl request))
