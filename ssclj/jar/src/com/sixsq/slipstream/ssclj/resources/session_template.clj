(ns com.sixsq.slipstream.ssclj.resources.session-template
  (:require
    [clojure.tools.logging :as log]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.util.response :as r]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const resource-tag :sessionTemplates)

(def ^:const resource-name "SessionTemplate")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "SessionTemplateCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ADMIN"
                            :type      "ROLE"
                            :right     "ALL"}
                           {:principal "ANON"
                            :type      "ROLE"
                            :right     "VIEW"}
                           {:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}]})

(def desc-acl {:owner {:principal "ADMIN"
                       :type      "ROLE"}
               :rules [{:principal "ANON"
                        :type      "ROLE"
                        :right     "VIEW"}]})

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; atom to keep track of the SessionTemplate descriptions
;;
(def descriptions (atom {}))

(defn register
  "Registers a given SessionTemplate description with the server. The
   description (desc) must be valid. The authentication method must be used as
   the id. The description can be looked up via the id, e.g. 'internal'."
  [id desc]
  (when (and id desc)
    (let [full-desc (assoc desc :acl desc-acl)]
      (swap! descriptions assoc id full-desc))
    (log/info "loaded SessionTemplate description" id)))

;;
;; schemas
;;

(def SessionTemplateDescription
  (merge c/CommonParameterDescription
         {:method      {:displayName "Authentication Method"
                        :category    "general"
                        :description "method to be used to authenticate user"
                        :type        "string"
                        :mandatory   true
                        :readOnly    true
                        :order       0}
          :methodKey   {:displayName "Authentication Method Key (Name)"
                        :category    "general"
                        :description "key used to identify this authentication source"
                        :type        "string"
                        :mandatory   true
                        :readOnly    false
                        :order       1}
          :redirectURI {:displayName "Redirect URI"
                        :category    "general"
                        :description "optional redirect URI to be used on success"
                        :type        "hidden"
                        :mandatory   false
                        :readOnly    false
                        :order       2}}))
;;
;; multimethods for validation
;;

(defmulti validate-subtype
          "Validates the given resource against the specific
           SessionTemplate subtype schema."
          :method)

(defmethod validate-subtype :default
  [resource]
  (throw (ex-info (str "unknown SessionTemplate type: " (:method resource)) resource)))

(defmethod crud/validate
  resource-uri
  [resource]
  (validate-subtype resource))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [{{:keys [method]} :body :as request}]
  (if (get @descriptions method)
    (add-impl request)
    (throw (r/ex-bad-request (str "invalid authentication method '" method "'")))))

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

;;
;; override the operations method to add describe action
;;

(defmethod crud/set-operations resource-uri
  [{:keys [id resourceURI] :as resource} request]
  (let [href (str id "/describe")]
    (try
      (a/can-modify? resource request)
      (let [ops (if (.endsWith resourceURI "Collection")
                  [{:rel (:add c/action-uri) :href id}]
                  [{:rel (:edit c/action-uri) :href id}
                   {:rel (:delete c/action-uri) :href id}
                   {:rel (:describe c/action-uri) :href href}])]
        (assoc resource :operations ops))
      (catch Exception e
        (if (.endsWith resourceURI "Collection")
          (dissoc resource :operations)
          (assoc resource :operations [{:rel (:describe c/action-uri) :href href}]))))))

;;
;; actions
;;
(defmethod crud/do-action [resource-url "describe"]
  [{{uuid :uuid} :params :as request}]
  (try
    (let [id (str resource-url "/" uuid)]
      (if-let [{:keys [method] :as resource} (crud/retrieve-by-id id {:user-name "INTERNAL" :user-roles ["ADMIN"]})]
        (if (a/can-view? resource request)
          (if-let [desc (get @descriptions method)]
            (r/json-response desc)
            (r/ex-not-found id)))
        (r/ex-not-found id)))
    (catch ExceptionInfo ei
      (ex-data ei))))


