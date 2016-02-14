(ns com.sixsq.slipstream.ssclj.resources.license-template
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-tag :licenseTemplate)

(def ^:const resource-name "LicenseTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "LicenseTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})
;;
;; schemas
;;

(def LicenseTemplate
  (merge c/CommonAttrs
         c/AclAttr
         {:licenseData c/NonBlankString}))

(def LicenseTemplateAttrs
  {(s/optional-key :licenseData) c/NonBlankString})

(def LicenseTemplateRef
  (s/constrained
    (merge LicenseTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    seq 'not-empty?))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn LicenseTemplate))
(defmethod crud/validate resource-uri
           [resource]
  (validate-fn resource))

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
