(ns com.sixsq.slipstream.ssclj.resources.user-identifier
  "
This resource represents the relationship between a unique identifier and a
SlipStream user. This is used by the SlipStream authentication mechanisms to
allow multiple authentication mechanisms to be mapped to the same account.
These resources are managed by the server; normally, administrators and users
will not interact with these resources directly.
"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.resource-metadata :as md]
    [com.sixsq.slipstream.ssclj.resources.spec.user-identifier :as ui-spec]
    [com.sixsq.slipstream.ssclj.util.metadata :as gen-md]
    [superstring.core :as str]))

(def ^:const resource-name "UserIdentifier")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "UserIdentifierCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}})

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn ::ui-spec/user-identifier))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))


;;
;; acl must allow users to see their own identifiers
;;

(defn user-acl
  [user-id]
  (let [[_ user] (u/split-resource-id user-id)]
    {:owner {:principal "ADMIN"
             :type      "ROLE"}
     :rules [{:principal user
              :type      "USER"
              :right     "VIEW"}]}))


(defmethod crud/add-acl resource-uri
  [{{user-id :href} :user :as resource} request]
  (assoc resource :acl (user-acl user-id)))


;;
;; ids for these resources are the hashed :identifier value
;;

(defmethod crud/new-identifier resource-name
  [{:keys [identifier] :as resource} resource-name]
  (->> identifier
       u/md5
       (str resource-url "/")
       (assoc resource :id)))

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


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::ui-spec/user-identifier)
  (md/register (gen-md/generate-metadata ::ns ::ui-spec/user-identifier)))
