(ns com.sixsq.slipstream.ssclj.resources.resource-metadata
  "This resource provides metadata associated with other CIMI resourced. It
   can be used to understand the attributes, allow values, actions, and
   capabilities. This information is linked to a resource through the typeURI
   attribute."
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata :as resource-metadata]
    [superstring.core :as str]))

(def ^:const resource-name "ResourceMetadata")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name (str resource-name "Collection"))

(def ^:const resource-uri (str c/cimi-schema-uri resource-name))

(def ^:const collection-uri (str c/cimi-schema-uri collection-name))

(def default-resource-acl {:owner {:principal "ADMIN"
                                   :type      "ROLE"}
                           :rules [{:principal "ADMIN"
                                    :type      "ROLE"
                                    :right     "MODIFY"}
                                   {:principal "ANON"
                                    :type      "ROLE"
                                    :right     "VIEW"}]})


(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}]})


;;
;; resource identifier is the MD5 checksum of the typeURI
;;
(defmethod crud/new-identifier resource-name
  [{:keys [typeURI] :as resource} resource-name]
  (->> typeURI
       u/md5
       (str resource-url "/")
       (assoc resource :id)))


;;
;; normally resource metadata should be accessible to anyone
;;
(defmethod crud/add-acl resource-uri
  [{:keys [acl] :as resource} request]
  (assoc resource :acl (or acl default-resource-acl)))


(def validate-fn (u/create-spec-validation-fn ::resource-metadata/resource-metadata))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))


(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))
(defmethod crud/add resource-name
  [request]
  (add-impl request))


(def retrieve-impl (std-crud/retrieve-fn resource-name))
(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))


(def delete-impl (std-crud/delete-fn resource-name))
(defmethod crud/delete resource-name
  [request]
  (delete-impl request))


(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::resource-metadata/resource-metadata))
