;;
;; Elastic Search implementation of Binding protocol
;;
(ns com.sixsq.slipstream.db.es.binding
  (:require
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.util.response :as response]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.es.acl :as acl]
    [com.sixsq.slipstream.db.binding :refer [Binding]])
  (:import (org.elasticsearch.index.engine VersionConflictEngineException)
           (java.io Closeable)))


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
  [{:keys [id] :as data}]
  (let [[collection-id uuid] (cu/split-id (:id data))
        json (prepare-data data)]
    [id collection-id uuid json]))


(defn doc->data
  "Converts to edn and renormalize ACLs"
  [doc]
  (-> doc
      esu/json->edn
      acl/normalize-acl))


(defn- throw-if-not-found
  [data id]
  (or data (throw (response/ex-not-found id))))


(defn- find-data
  [client index id options]
  (let [[type docid] (cu/split-id id)]
    (-> (esu/read client index type docid options)
        (.getSourceAsString)
        doc->data
        (throw-if-not-found id))))


(defn- add-data
  [data]
  (let [[id collection-id uuid json] (data->doc data)]
    (try
      (if (esu/create *client* index-name collection-id uuid json)
        (response/response-created id)
        (response/response-error "resource not created"))
      (catch VersionConflictEngineException _
        (response/response-conflict id)))))


(deftype ESBinding []
  Binding

  (add [_ data options]
    (add-data data))


  (add [_ collection-id data options]
    (add-data data))


  (retrieve [_ id options]
    (find-data *client* index-name id options))


  (delete [_ {:keys [id]} options]
    (find-data *client* index-name id options)
    (let [[type docid] (cu/split-id id)]
      (.status (esu/delete *client* index-name type docid)))
    (response/response-deleted id))


  (edit [_ {:keys [id] :as data} options]
    (let [[type docid] (cu/split-id id)
          updated-doc (prepare-data data)]
      (if (esu/update *client* index-name type docid updated-doc)
        (response/json-response data)
        (response/response-conflict id))))


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
      [meta hits]))

  Closeable
  (close [_]
    nil))


(defn get-instance
  []
  (ESBinding.))
