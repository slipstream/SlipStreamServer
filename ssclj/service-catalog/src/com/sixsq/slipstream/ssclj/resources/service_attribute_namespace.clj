(ns com.sixsq.slipstream.ssclj.resources.service-attribute-namespace
  (:require
    [schema.core :as s]
    [superstring.core :as str]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-name "ServiceAttributeNamespace")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ServiceAttributeNamespaceCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; schemas
;;

(defn- blank-or-dot-or-slash?
  [s]
  (or (str/blank? s)
      (some #{\. \/} s)))

(def NoDotNoSlash
  (s/constrained s/Str (complement blank-or-dot-or-slash?)))

(def ServiceNamespace
  (merge c/CommonAttrs
         c/AclAttr
         {:prefix  NoDotNoSlash
          :uri     c/NonBlankString}))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn ServiceNamespace))
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

(defn- colliding-namespace?
  [prefix uri ns]
  (or (= prefix (:prefix ns)) (= uri (:uri ns))))

(defn all-namespaces
  []
  (-> (crud/query
        {:identity       {:current         "slipstream",
                          :authentications {"slipstream"
                                            {:identity "slipstream"}}}
         :params         {:resource-name resource-url}
         :user-roles     ["ADMIN"]
         :request-method :get})
      (get-in [:body :serviceAttributeNamespaces])))

(defn all-prefixes
  []
  (map :prefix (all-namespaces)))

(defn- colliding-ids
  [prefix uri]
  (->> (all-namespaces)
       (filter (partial colliding-namespace? prefix uri))
       (map :id)))

(defmethod crud/add resource-name
  [request]
  (let [prefix        (get-in request [:body :prefix])
        uri           (get-in request [:body :uri])
        forbiding     (colliding-ids prefix uri)]
    (if-not (empty? forbiding)
      (esb/response-conflict (first forbiding))
      (add-impl request))))

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


