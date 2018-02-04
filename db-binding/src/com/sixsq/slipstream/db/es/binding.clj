;;
;; Elastic Search implementation of Binding protocol
;;
(ns com.sixsq.slipstream.db.es.binding
  (:require
    [ring.util.response :as r]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.utils.responses :as responses]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.es.acl :as acl]
    [com.sixsq.slipstream.db.binding :refer [Binding]])
  (:import (org.elasticsearch.index.engine VersionConflictEngineException)))

(def ^:const index-name "resources-index")

(defn wait-client-create-index
  [client]
  (esu/wait-for-cluster client)
  (when-not (esu/index-exists? client index-name)
    (esu/create-index client index-name)
    (esu/wait-for-index client index-name))
  client)

(defn create-client
  []
  (wait-client-create-index (esu/create-es-client)))

(def ^:dynamic *client*)

(defn set-client!
  [client]
  (alter-var-root #'*client* (constantly client)))

(defn unset-client!
  []
  (.unbindRoot #'*client*))

(defn close-client!
  []
  (.close *client*)
  (unset-client!))

(defn force-admin-role-right-all
  [data]
  (update-in data [:acl :rules] #(vec (set (conj % {:type "ROLE" :principal "ADMIN" :right "ALL"})))))

(defn- prepare-data
  "Prepares the data by adding the ADMIN role with ALL rights, denormalizing
   the ACL, and turning the document into JSON."
  [data]
  (-> data
      force-admin-role-right-all
      acl/denormalize-acl
      esu/edn->json))

(defn- data->doc
  "Provides the tuple of id, uuid, and prepared JSON document. "
  [data]
  (let [id (:id data)
        uuid (second (cu/split-id id))
        json (prepare-data data)]
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
      (responses/map-response 201 id)
      (r/header "Location" id)))

(defn response-error
  []
  (responses/map-response "Resource not created" 500 nil))

(defn response-conflict
  [id]
  (responses/map-response (str "Conflict for " id) 409 id))

(defn- response-deleted
  [id]
  (responses/map-response (str id " deleted") 200 id))

(defn- response-not-found
  [id]
  (responses/map-response (str id " not found") 404 id))

(defn- response-updated
  [id]
  (responses/map-response (str "updated " id) 200 id))

(defn- throw-if-not-found
  [data id]
  (if-not data
    (throw (responses/ex-not-found id))
    data))

(defn- find-data
  [client index id options]
  (let [[type docid] (cu/split-id id)]
    (-> (esu/read client index type docid options)
        (.getSourceAsString)
        doc->data
        (throw-if-not-found id))))

(deftype ESBinding []
  Binding

  (add [_ type data options]
    (let [[id uuid json] (data->doc data)]
      (try
        (if (esu/create *client* index-name (cu/de-camelcase type) uuid json)
          (response-created id)
          (response-error))
        (catch VersionConflictEngineException _
          (response-conflict id)))))


  (retrieve [_ id options]
    (find-data *client* index-name id options))


  (delete [_ {:keys [id]} options]
    (find-data *client* index-name id options)
    (let [[type docid] (cu/split-id id)]
      (.status (esu/delete *client* index-name type docid)))
    (response-deleted id))


  (edit [_ {:keys [id] :as data} options]
    (let [[type docid] (cu/split-id id)
          updated-doc (prepare-data data)]
      (if (esu/update *client* index-name type docid updated-doc)
        (responses/json-content-type data)
        (response-conflict id))))


  (query [_ collection-id options]
    (let [result (esu/search *client* index-name collection-id options)
          count-before-pagination (-> result :hits :total)
          aggregations (:aggregations result)
          meta (cond-> {:count count-before-pagination}
                       aggregations (assoc :aggregations aggregations))
          hits (->> result
                    :hits
                    :hits
                    (map :_source)
                    (map acl/normalize-acl))]
      [meta hits])))


(defn get-instance
  []
  (ESBinding.))
