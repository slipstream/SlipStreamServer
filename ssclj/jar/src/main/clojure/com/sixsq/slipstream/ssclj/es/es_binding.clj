;;
;; Elastic Search implementation of Binding protocol
;;
(ns com.sixsq.slipstream.ssclj.es.es-binding
  (:require
    [ring.util.response :as r]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.es.es-util :as esu]
    [com.sixsq.slipstream.ssclj.es.acl :as acl]
    )
  (:import (com.sixsq.slipstream.ssclj.db.binding Binding)
           (org.elasticsearch.index.engine DocumentAlreadyExistsException)))

(def ^:const ^:private index "resources-index")

(defn create-client
  []
  (println "Creating ES client")
  (let [node (esu/create-test-node)
        client (esu/node-client node)]
    (esu/wait-for-cluster client)
    (esu/create-index client index)
    (esu/wait-for-index client index)
    client))

(defonce client (create-client))

(defn- force-admin-role-right-all
  [data]
  (update-in data [:acl :rules] #(vec (set (conj % {:type "ROLE" :principal "ADMIN" :right "ALL"})))))

(defn- data->doc
  "Prepares data before insertion in index
  That includes
  - assoc id (which is type/uuid)
  - add ADMIN role with right ALL
  - denormalize ACLs
  - jsonify "
  [type data]
  (let [uuid  (cu/random-uuid)
        id    (str type "/" uuid)
        json  (-> data
                  force-admin-role-right-all
                  acl/denormalize-acl
                  (assoc :id id)
                  esu/edn->json)]
    [id uuid json]))

(defn doc->data
  "Converts to edn and renormalize ACLs"
  [doc]
  (-> doc
      esu/json->edn
      acl/normalize-acl))

(defn- response-created
  [id]
  (-> (str "created " id)
      (cu/map-response 201 id)
      (r/header "Location" id)))

;; TODO
;; 409 if already existing
;; other code when appropriate
(defn response-error
  []
  (-> "Resource not created"
      (cu/map-response 500 nil)))

(defn response-conflict
  [id]
  (cu/ex-conflict id))

(defn- check-identity-present
  [options]
  (when (and (empty? (:user-name options))
             (every? empty? (:user-roles options)))
    (throw (IllegalArgumentException. "A non empty user name or user role is mandatory."))))

(defn find-data
  [client index id options action]
  (check-identity-present options)
  (let [[type docid] (split-id id)]
    (-> (esu/read client index type docid)
        (.getSourceAsString)
        doc->data
        (acl/check-can-do options action))))

(deftype ESBinding []
  Binding
  (add [_ type data options]
    (let [[id uuid json] (data->doc type data)]
      (try
      (if (esu/create client index type uuid json)
        (response-created id)
        (response-error))
      (catch DocumentAlreadyExistsException e
        (response-conflict 123))))) ;; TODO Retrieve id from exception

  (retrieve [this id options]
    ;; (check-exist id) TODO equivalent
    (find-data client index id options "VIEW"))

  (delete [this {:keys [id]}]
    (check-exist id)
    (delete-resource id)
    (response-deleted id))

  (edit [this {:keys [id] :as data}]
    (check-exist id)
    (update-resource id data)
    (response-updated id))

  (query [this collection-id options]
    (find-resources collection-id options)))

(defn get-instance []
  (init-es)
  (ESBinding.))
