(ns com.sixsq.slipstream.ssclj.resources.virtual-machine
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.virtual-machine]

    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.log :as logu]))

(def ^:const resource-tag :virtualMachines)

(def ^:const resource-name "VirtualMachine")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "VirtualMachineCollection")

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

(defn create-acl
  [id]
  {:owner {:principal "ADMIN"
           :type      "ROLE"}
   :rules [{:principal id
            :type      "USER"
            :right     "VIEW"}]})




(def validate-fn (u/create-spec-validation-fn :cimi/virtual-machine))
(defmethod crud/validate
  resource-uri
  [resource]
  (validate-fn resource))


(defmethod crud/add-acl resource-uri
  [{:keys [acl] :as resource} request]
  (if acl
    resource
    (let [user-id (:identity (a/current-authentication request))
          run-owner (subs (-> request
                              :body
                              :deployment
                              :user
                              :href
                              )
                          (count "/user"))

          ]
      (if run-owner
        (assoc resource :acl (create-acl run-owner))
        (assoc resource :acl (create-acl user-id))
        )

      )
    )
  )

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
