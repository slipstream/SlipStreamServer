(ns com.sixsq.slipstream.ssclj.resources.deployment-parameter
  (:require
    [com.sixsq.slipstream.auth.acl :as a]

    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-parameter :as deployment-parameter]
    [superstring.core :as str]
    [clojure.string :as s]))

(def ^:const resource-name "DeploymentParameter")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "DeploymentCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

;; only authenticated users can view and create credentials
(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ADMIN"
                              :type      "ROLE"
                              :right     "MODIFY"}
                             {:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

;;
;; multimethod for ACLs
;;

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))


(def validate-fn (u/create-spec-validation-fn ::deployment-parameter/deployment-parameter))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))

;;
;; set the resource identifier to "deployment-parameter/predictable-uuid3-from-string"
;;
(defmethod crud/new-identifier resource-name
  [{:keys [deployment nodeID name] :as json} resource-name]
  (let [id (s/join ":" [(:href deployment) nodeID name])]
    (assoc json :id (str resource-url "/" (u/from-data-uuid id)))))

;;
;; CRUD operations
;;
(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))
(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def edit-impl (std-crud/edit-fn resource-name))
(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

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


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::deployment-parameter/deployment-parameter))
