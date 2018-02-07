(ns com.sixsq.slipstream.ssclj.resources.service-benchmark
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.service-benchmark]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.service-catalog.utils :as sc]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as sn]
    [superstring.core :as str]))


(def ^:const resource-name "ServiceBenchmark")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ServiceBenchmarkCollection")

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

(defn- validate-attributes
  [resource]
  (let [valid-prefixes (sn/all-prefixes)
        resource-payload (dissoc resource :acl :id :resourceURI :name :description
                                 :created :updated :properties :operations :credentials :serviceOffer)
        validator (partial sc/valid-attribute-name? valid-prefixes)]
    (if (sc/valid-attributes? validator resource-payload)
      resource
      (sc/throw-wrong-namespace))))

;
(def validate-fn (u/create-spec-validation-fn :cimi/service-benchmark))
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
