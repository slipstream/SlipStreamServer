(ns com.sixsq.slipstream.ssclj.resources.session
  (:require
    [com.sixsq.slipstream.ssclj.resources.session-template :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [schema.core :as s]
    [superstring.core :as ss])
  (:import (java.util Date TimeZone)
           (java.text SimpleDateFormat)))

(def ^:const resource-tag :sessions)

(def ^:const resource-name "Session")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "SessionCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def ^:const create-uri (str c/slipstream-schema-uri resource-name "Create"))

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
;; schemas
;;

(def Session
  (merge c/CommonAttrs
         c/AclAttr
         {:authnMethod                  c/NonBlankString
          :username                     c/NonBlankString
          (s/optional-key :virtualHost) c/NonBlankString
          (s/optional-key :clientIP)    c/NonBlankString
          :expiry                       c/Timestamp}))

(def SessionCreate
  (merge c/CreateAttrs
         {:sessionTemplate tpl/SessionTemplateRef}))

;;
;; validate subclasses of sessions
;;

(defmulti validate-subtype
          :authnMethod)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown Session type: " (:authnMethod resource)) resource)))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))

;;
;; validate create requests for subclasses of sessions
;;

(defn dispatch-on-authn-method [resource]
  (get-in resource [:sessionTemplate :authnMethod]))

(defmulti create-validate-subtype dispatch-on-authn-method)

(defmethod create-validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown Session create type: " (dispatch-on-authn-method resource) resource) resource)))

(defmethod crud/validate create-uri
  [resource]
  (create-validate-subtype resource))

;;
;; multimethod for ACLs
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; template processing
;;

(defn dispatch-conversion
  [resource _]
  (:authnMethod resource))

(defmulti tpl->session dispatch-conversion)

;; default implementation just updates the resourceURI
(defmethod tpl->session :default
  [resource request]
  [nil (assoc resource :resourceURI resource-uri)])

;;
;; CRUD operations
;;

(defn add-impl [{:keys [id body] :as request}]
  (a/can-modify? {:acl collection-acl} request)
  (db/add
    resource-name
    (-> body
        u/strip-service-attrs
        (assoc :id id)
        (assoc :resourceURI resource-uri)
        u/update-timestamps
        (crud/add-acl request)
        crud/validate)
    {}))

;; requires a SessionTemplate to create new Session
(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [idmap {:identity (:identity request)}
        [cookie-header body] (-> body
                                 (assoc :resourceURI create-uri)
                                 (std-crud/resolve-hrefs idmap)
                                 (crud/validate)
                                 (:sessionTemplate)
                                 (tpl->session request))]
    (-> (assoc request :id (:id body) :body body)
        add-impl
        (merge cookie-header))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def edit-impl (std-crud/edit-fn resource-name))

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(def ^:private sdf
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z")
    (.setTimeZone (TimeZone/getTimeZone "GMT"))))

(defn now-gmt
  []
  (.format sdf (Date.)))

(defn cookie-name [{:keys [body]}]
  (str "slipstream." (ss/replace (:resource-id body) "/" ".")))

(defn delete-cookie [{:keys [status] :as response}]
  (if (= status 200)
    {:cookies {(cookie-name response) {:value {:token "INVALID"} :path "/" :max-age 0 :expires (now-gmt)}}}
    {}))

(defmethod crud/delete resource-name
  [request]
  (let [response (delete-impl request)
        cookies (delete-cookie response)]
    (merge response cookies)))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))

(defmethod crud/add-acl resource-uri
  [{:keys [username] :as resource} _]
  (assoc resource :acl {:owner {:principal username
                                :type      "USER"}
                        :rules [{:principal username
                                 :type      "USER"
                                 :right     "MODIFY"}]}))
