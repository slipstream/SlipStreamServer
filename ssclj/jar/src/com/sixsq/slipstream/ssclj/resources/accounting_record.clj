(ns com.sixsq.slipstream.ssclj.resources.accounting-record
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.credential-template-username-password :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.log :as logu]))

(def ^:const resource-tag :accountingRecords)

(def ^:const resource-name "AccountingRecord")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "AccountingRecordCollection")

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
                              :right     "VIEW"}
                             ]})

;;
;; validate the created credential resource
;; must dispatch on the type because each credential has a different schema
;;
(defmulti validate-subtype :type)

(defmethod validate-subtype :default
  [resource]
  (logu/log-and-throw-400 (str "unknown AccountingRecord type: '" (:type resource) "'")))

(defmethod crud/validate resource-uri
  [resource]
  (validate-subtype resource))



(def ^:const view-accounting-role "view_accounting")
;;
;; multimethod for ACLs
;;

(defn create-acl
  [id realm]

  (let [default-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal id
                              :type      "USER"
                              :right     "VIEW"}

                             ]}]
    (cond
      realm (update-in default-acl [:rules] conj {:principal (str realm ":" view-accounting-role)
                                                  :type      "ROLE"
                                                  :right     "VIEW"})
      :else default-acl
      )
    )
  )

(defmethod crud/add-acl resource-uri
  [{:keys [user realm] :as resource} request]
  (assoc resource :acl (create-acl user realm))
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

