(ns com.sixsq.slipstream.ssclj.resources.session-template
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-tag :sessionTemplates)

(def ^:const resource-name "SessionTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "SessionTemplateCollection")

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

(def SessionCredentials
  (s/constrained
    {s/Keyword c/NonBlankString}
    seq 'not-empty?))

(def SessionTemplate
  (merge c/CommonAttrs
         c/AclAttr
         {:authn-method                 c/NonBlankString
          (s/optional-key :logo)        c/ResourceLink
          (s/optional-key :credentials) SessionCredentials}))

(def SessionTemplateAttrs
  {(s/optional-key :authn-method) c/NonBlankString
   (s/optional-key :logo)         c/ResourceLink
   (s/optional-key :credentials)  SessionCredentials})

(def SessionTemplateRef
  (s/constrained
    (merge SessionTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    seq 'not-empty?))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn SessionTemplate))
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
