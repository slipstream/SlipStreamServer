(ns com.sixsq.slipstream.ssclj.resources.session
  (:require
    [com.sixsq.slipstream.ssclj.resources.session-template :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-tag :sessions)

(def ^:const resource-name "Session")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "SessionCollection")

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

(def Session
  (merge c/CommonAttrs
         c/AclAttr
         {:owner     c/NonBlankString
          :type      c/NonBlankString
          :expiry    c/Timestamp
          :userLimit c/NonNegInt}))

(def SessionCreate
  (merge c/CreateAttrs
         {:licenseTemplate tpl/SessionTemplateRef}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Session))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn SessionCreate))
(defmethod crud/validate create-uri
  [resource]
  (create-validate-fn resource))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; template processing
;;

(defn tpl->session
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

;; requires a SessionTemplate to create new Session
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [body (-> body
                 (assoc :resourceURI create-uri)
                 (crud/validate)
                 (:licenseTemplate)
                 (tpl->session))]
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
