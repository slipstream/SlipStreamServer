(ns com.sixsq.slipstream.ssclj.resources.session
  (:require
    [com.sixsq.slipstream.ssclj.resources.session-template :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.auth.auth :as auth]
    [schema.core :as s]))

(def ^:const resource-tag :sessions)

(def ^:const resource-name "Session")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "SessionCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "ALL"}]})
;;
;; schemas
;;

(def Session
  (merge c/CommonAttrs
         c/AclAttr
         {:username     c/NonBlankString
          :authn-method c/NonBlankString
          :last-active  c/Timestamp}))

(def SessionCreate
  (merge c/CreateAttrs
         {:sessionTemplate tpl/SessionTemplateRef}))

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

(defn add-credentials-to-request [request tpl]
  (let [{:keys [authn-method credentials]} tpl]
    (->> request
         :params
         (merge credentials)
         (assoc :authn-method authn-method)
         (assoc request :params))))

(defn do-login [request]
  (let [{:keys [status cookies]} (auth/login request)]
    (if (not= status 200)
      (throw (ex-info "login failed" {:status status :message "login failed"}))
      ["dummy" cookies])))

(defn tpl->session
  [tpl request]
  (let [tpl (-> tpl
                std-crud/resolve-hrefs
                tpl/validate-fn)
        updated-request (add-credentials-to-request request tpl)
        [username cookies] (do-login updated-request)]
    {:cookies cookies
     :body    {:username     username
               :authn-method (:authn-method tpl)
               :last-active  (u/now)}}))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a SessionTemplate to create new Session
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [tpl (-> body
                (assoc :resourceURI create-uri)
                (crud/validate)
                (:sessionTemplate))
        {:keys [cookies body]} (tpl->session tpl request)]
    (-> body
        (assoc request :body)
        (add-impl)
        (assoc :cookies cookies))))

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
