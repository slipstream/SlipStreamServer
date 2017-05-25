(ns com.sixsq.slipstream.ssclj.resources.credential
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.credential-template-username-password :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.log :as logu]))

(def ^:const resource-tag :credentials)

(def ^:const resource-name "Credential")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "CredentialCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

;; only authenticated users can view and create credentials
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; description
;; FIXME: Must be visible as an action on credential resources.
;;
(def CredentialDescription tpl/desc)
(def ^:const desc CredentialDescription)

;;
;; validate the created credential resource
;; must dispatch on the type because each credential has a different schema
;;
(defmulti validate-subtype :type)

(defmethod validate-subtype :default
  [resource]
  (logu/log-and-throw-400 (str "unknown Credential type: '" (:type resource) "'")))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of credentials
;; different credentials take different inputs
;;
(defn dispatch-on-registration-method [resource]
  (get-in resource [:credentialTemplate :type]))

(defmulti create-validate-subtype dispatch-on-registration-method)

(defmethod create-validate-subtype :default
  [resource]
  (logu/log-and-throw-400 "missing or invalid CredentialTemplate reference"))

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
   :rules [{:principal id
            :type      "USER"
            :right     "MODIFY"}]})

(defmethod crud/add-acl resource-uri
  [{:keys [acl] :as resource} request]
  (if acl
    resource
    (let [user-id (:identity (a/current-authentication request))]
      (assoc resource :acl (create-acl user-id)))))

;;
;; template processing
;;

(defn dispatch-conversion
  [resource _]
  (:type resource))

(defmulti tpl->credential dispatch-conversion)

;; default implementation throws if the credential type is unknown
(defmethod tpl->credential :default
  [resource request]
  (logu/log-and-throw-400 "missing or invalid CredentialTemplate reference"))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; requires a CredentialTemplate to create new Credential
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        body (-> body
                 (assoc :resourceURI create-uri)
                 (update-in [:credentialTemplate] dissoc :type) ;; forces use of template reference
                 (std-crud/resolve-hrefs idmap)
                 (crud/validate)
                 (:credentialTemplate)
                 (tpl->credential request))]
    (add-impl (assoc request :id (:id body) :body body))))

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

