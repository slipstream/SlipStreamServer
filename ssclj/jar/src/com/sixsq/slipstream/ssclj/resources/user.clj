(ns com.sixsq.slipstream.ssclj.resources.user
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.user-template-direct :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]))

(def ^:const resource-tag :users)

(def ^:const resource-name "User")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "UserCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

;; creating a new user is a registration request, so anonymous users must
;; be able to view the collection and post requests to it (if a template is
;; visible to ANON.)
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "ANON"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; description
;; FIXME: Must be visible as an action on user resources.
;;
(def UserDescription tpl/desc)
(def ^:const desc UserDescription)

;;
;; validate the created user resource
;; all create (registration) requests produce user resources with the same schema
;;
(def validate-fn (u/create-spec-validation-fn :cimi/user))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

;;
;; validate create requests for subclasses of users
;; different create (registration) requests may take different inputs
;;
(defn dispatch-on-registration-method [resource]
  (get-in resource [:userTemplate :method]))

(defmulti create-validate-subtype dispatch-on-registration-method)

(defmethod create-validate-subtype :default
  [resource]
  (u/log-and-throw-400 "missing or invalid UserTemplate reference"))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))

;;
;; multimethod for ACLs
;;

(defn create-acl
  [id]
  {:owner {:principal "ADMIN"
           :type      "ROLE"}
   :rules [{:principal "ADMIN"
            :type      "ROLE"
            :right     "ALL"}
           {:principal id
            :type      "USER"
            :right     "MODIFY"}]})

(defmethod crud/add-acl resource-uri
  [{:keys [username acl] :as resource} request]
  (assoc
    resource
    :acl
    (or acl (create-acl username))))

;;
;; set the resource identifier to "user/username"
;;
(defmethod crud/new-identifier resource-name
  [{:keys [username] :as json} resource-name]
  (assoc json :id (str resource-url "/" username)))

;;
;; template processing
;;

(defn dispatch-conversion
  [resource _]
  (:method resource))

(defmulti tpl->user dispatch-conversion)

;; default implementation throws if the registration method is unknown
(defmethod tpl->user :default
  [resource request]
  (u/log-and-throw-400 "missing or invalid UserTemplate reference"))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a UserTemplate to create new User
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        body (-> body
                 (assoc :resourceURI create-uri)
                 (update-in [:userTemplate] dissoc :method) ;; forces use of template reference
                 (std-crud/resolve-hrefs idmap)
                 (crud/validate)
                 (:userTemplate)
                 (tpl->user request))]
    (add-impl (assoc request :id (:id body) :body body))))  ;; FIXME: WRONG, need to have id=username

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

