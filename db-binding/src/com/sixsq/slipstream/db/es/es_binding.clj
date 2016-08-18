;;
;; Elastic Search implementation of Binding protocol
;;
(ns com.sixsq.slipstream.db.es.es-binding
  (:require
    [ring.util.response :as r]
    [superstring.core :as s]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.es.acl :as acl]
    [com.sixsq.slipstream.db.binding :refer [Binding]])
  (:import (org.elasticsearch.index.engine DocumentAlreadyExistsException)
           (clojure.lang ExceptionInfo)))

(def ^:const index-name "resources-index")

(defn- wait-client-create-index
  [client]
  (esu/wait-for-cluster client)
  (when-not (esu/index-exists? client index-name)
    (esu/create-index client index-name)
    (esu/wait-for-index client index-name))
  client)

(defn create-client
  []
  (wait-client-create-index (esu/create-es-client)))

(defn create-test-client
  []
  (wait-client-create-index (esu/create-test-es-client)))

(def ^:dynamic *client*)

(defn set-client!
  [client]
  (alter-var-root #'*client* (constantly client)))

(defn force-admin-role-right-all
  [data]
  (update-in data [:acl :rules] #(vec (set (conj % {:type "ROLE" :principal "ADMIN" :right "ALL"})))))

(defn- split-id
  "Split id in [type docid].
  id is usually in the form type/docid.
  Exception for cloud-entry-point: in this case id is only type (there is only one cloud-entry-point)"
  [id]
  (let [[type docid] (s/split id #"/")]
    [type (if docid docid type)]))

(defn- data->doc
  "Prepares data before insertion in index
  That includes
  - add ADMIN role with right ALL
  - denormalize ACLs
  - jsonify "
  [data]
  (let [id (:id data)
        uuid (second (split-id id))
        json (-> data
                 force-admin-role-right-all
                 acl/denormalize-acl
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

(defn response-error
  []
  (cu/map-response "Resource not created" 500 nil))

(defn response-conflict
  [id]
  (cu/map-response (str "Conflict for " id) 409 id))

(defn- response-deleted
  [id]
  (cu/map-response (str id " deleted") 200 id))

(defn- response-not-found
  [id]
  (cu/map-response (str id " not found") 404 id))

(defn- response-updated
  [id]
  (cu/map-response (str "updated " id) 200 id))

(defn- throw-if-not-found
  [data id]
  (if-not data
    (throw (cu/ex-not-found id))
    data))

(defn- find-data
  [client index id options action]
  (let [[type docid] (split-id id)]
    (-> (esu/read client index type docid)
        (.getSourceAsString)
        doc->data
        (throw-if-not-found id)
        (acl/check-can-do options action))))

(deftype ESBinding []
  Binding
  (add [_ type data options]
    (let [[id uuid json] (data->doc data)]
      (try
        (if (esu/create *client* index-name (cu/de-camelcase type) uuid json)
          (response-created id)
          (response-error))
        (catch DocumentAlreadyExistsException e
          (response-conflict id)))))

  (retrieve [_ id options]
    (find-data *client* index-name id options "VIEW"))

  (delete [_ {:keys [id]} options]
    (find-data *client* index-name id options "ALL")
    (let [[type docid] (split-id id)]
      (.isFound (esu/delete *client* index-name type docid)))
    (response-deleted id))

  (edit [_ {:keys [id] :as data} options]
    (find-data *client* index-name id options "MODIFY")
    (let [[type docid] (split-id id)]
      (if (esu/update *client* index-name type docid (esu/edn->json data))
        (cu/json-response data)
        (response-conflict id))))

  (query [_ collection-id options]
    (let [response (esu/search *client* index-name collection-id options)
          result (esu/json->edn (str response))
          count-before-pagination (-> result :hits :total)
          hits (->> (-> result :hits :hits)
                    (map :_source)
                    (map acl/normalize-acl))]
      [count-before-pagination hits])))

(defn get-instance
  []
  (ESBinding.))
