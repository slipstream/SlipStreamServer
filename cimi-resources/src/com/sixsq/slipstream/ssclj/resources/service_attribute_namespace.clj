(ns com.sixsq.slipstream.ssclj.resources.service-attribute-namespace
  (:require
    [superstring.core :as str]
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.ssclj.resources.spec.service-attribute-namespace]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.util.response :as response]
    [com.sixsq.slipstream.db.filter.parser :as parser]))

(def ^:const resource-name "ServiceAttributeNamespace")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ServiceAttributeNamespaceCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}]})

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn :cimi/service-attribute-namespace))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-uri
  [resource request]
  (assoc resource :acl resource-acl))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

;; FIXME: Roles are needed in two locations!  Should be unique way to specify authentication information.
(def ^:private all-query-map {:identity       {:current         "slipstream",
                                               :authentications {"slipstream"
                                                                 {:identity "slipstream"
                                                                  :roles    ["ADMIN" "USER" "ANON"]}}}
                              :params         {:resource-name resource-url}
                              :user-roles     ["ADMIN" "USER" "ANON"]
                              :request-method :get})

(defn extract-field-values
  "returns a set of the values of the field k (as a keyword) from the
   ServiceAttributeNamespace resources that match the query"
  [query-map k]
  (->> query-map
       crud/query
       :body
       :serviceAttributeNamespaces
       (map k)
       set))

(defn all-prefixes
  []
  (extract-field-values all-query-map :prefix))

(defn colliding-id
  "returns the first ServiceAttributeNamespace resource that has the same prefix OR uri"
  [prefix uri]
  (let [filter (parser/parse-cimi-filter (format "(prefix='%s') or (uri='%s')" prefix uri))]
    (-> all-query-map
        (assoc :cimi-params {:filter filter :first 1 :last 1})
        (extract-field-values :id)
        first)))

(defmethod crud/add resource-name
  [{{:keys [prefix uri]} :body :as request}]
  (if-let [id (colliding-id prefix uri)]
    (response/response-conflict id)
    (add-impl request)))

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

(defmethod crud/new-identifier resource-name
  [json _]
  (let [new-id (str resource-url "/" (:prefix json))]
    (assoc json :id new-id)))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url :cimi/service-attribute-namespace))
