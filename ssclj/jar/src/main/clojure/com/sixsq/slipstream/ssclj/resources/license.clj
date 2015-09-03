(ns com.sixsq.slipstream.ssclj.resources.license
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.license-template :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-tag :licenses)

(def ^:const resource-name "License")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "LicenseCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})
;;
;; schemas
;;

(def License
  (merge c/CommonAttrs
         c/AclAttr
         {:owner     c/NonBlankString
          :type      c/NonBlankString
          :expiry    c/Timestamp
          :userLimit c/NonNegInt}))

(def LicenseCreate
  (merge c/CreateAttrs
         {:licenseTemplate tpl/LicenseTemplateRef}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn License))
(defmethod crud/validate resource-uri
           [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn LicenseCreate))
(defmethod crud/validate create-uri
           [resource]
  (create-validate-fn resource))

(defmethod crud/add-acl resource-uri
           [resource request]
  (a/add-acl resource request))

;;
;; template processing
;;

(defn tpl->license
  [{:keys [licenseData] :as tpl}]
  (let [tpl (-> tpl
                (std-crud/resolve-hrefs)
                (dissoc :licenseData))]
    (->> licenseData
         (u/decrypt)
         (u/decode-base64)
         (merge tpl))))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a LicenseTemplate to create new license
(defmethod crud/add resource-name
           [{:keys [body] :as request}]
  (let [body (-> body
                 (assoc :resourceURI create-uri)
                 (crud/validate)
                 (:licenseTemplate)
                 (tpl->license))]
    (add-impl (assoc request :body body))))

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
