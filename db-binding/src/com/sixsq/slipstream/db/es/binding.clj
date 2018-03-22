;;
;; Elasticsearch implementation of Binding protocol
;;
(ns com.sixsq.slipstream.db.es.binding
  (:require
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.util.response :as response]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.utils.acl :as acl-utils]
    [com.sixsq.slipstream.db.binding :refer [Binding]]
    [camel-snake-kebab.core :refer [->kebab-case]])
  (:import
    (org.elasticsearch.client Client)
    (org.elasticsearch.index.engine VersionConflictEngineException)
    (java.io Closeable)))


(def ^:const index-prefix "slipstream-")


(defn id->index
  [id]
  (->> id cu/split-id first (str index-prefix)))

(defn collection-id->index
  [collection-id]
  (str index-prefix (->kebab-case collection-id)))

(defn wait-client-create-index
  [client]
  (esu/wait-for-cluster client)
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


(defn- prepare-data
  "Prepares the data by adding the ADMIN role with ALL rights, denormalizing
   the ACL, and turning the document into JSON."
  [data]
  (-> data
      acl-utils/force-admin-role-right-all
      acl-utils/denormalize-acl
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
      acl-utils/normalize-acl))


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
  [client data]
  (let [[id collection-id uuid json] (data->doc data)]
    (try
      (if (esu/create client (collection-id->index collection-id) collection-id uuid json)
        (response/response-created id)
        (response/response-error "resource not created"))
      (catch VersionConflictEngineException _
        (response/response-conflict id)))))


(deftype ESBinding []
  Binding

  (initialize [_ collection-id options]
    (let [index (collection-id->index collection-id)]
      (when-not (esu/index-exists? *client* index)
        (esu/create-index *client* index)
        (esu/wait-for-index *client* index))))


  (add [_ data options]
    (add-data *client* data))


  (add [_ collection-id data options]
    (add-data *client* data))


  (retrieve [_ id options]
    (find-data *client* (id->index id) id options))


  (delete [_ {:keys [id]} options]
    (find-data *client* (id->index id) id options)
    (let [[type docid] (cu/split-id id)]
      (.status (esu/delete *client* (id->index id) type docid)))
    (response/response-deleted id))


  (edit [_ {:keys [id] :as data} options]
    (let [[type docid] (cu/split-id id)
          updated-doc (prepare-data data)]
      (if (esu/update *client* (id->index id) type docid updated-doc)
        (response/json-response data)
        (response/response-conflict id))))


  (query [_ collection-id options]
    (let [result (esu/search *client* (collection-id->index collection-id) collection-id options)
          count-before-pagination (-> result :hits :total)
          aggregations (:aggregations result)
          meta (cond-> {:count count-before-pagination}
                       aggregations (assoc :aggregations aggregations))
          hits (->> result
                    :hits
                    :hits
                    (map :_source)
                    (map acl-utils/normalize-acl))]
      [meta hits]))

  Closeable
  (close [_]
    nil))


(defn get-instance
  []
  (ESBinding.))

(deftype ESBindingLocal [^Client client]
  Binding

  (initialize [_ collection-id options]
    (let [index (collection-id->index collection-id)]
      (when-not (esu/index-exists? client index)
        (esu/create-index client index)
        (esu/wait-for-index client index))))


  (add [_ data options]
    (add-data client data))


  (add [_ collection-id data options]
    (add-data client data))


  (retrieve [_ id options]
    (find-data client (id->index id) id options))


  (delete [_ {:keys [id]} options]
    (find-data client (id->index id) id options)
    (let [[type docid] (cu/split-id id)]
      (.status (esu/delete client (id->index id) type docid)))
    (response/response-deleted id))


  (edit [_ {:keys [id] :as data} options]
    (let [[type docid] (cu/split-id id)
          updated-doc (prepare-data data)]
      (if (esu/update client (id->index id) type docid updated-doc)
        (response/json-response data)
        (response/response-conflict id))))


  (query [_ collection-id options]
    (let [result (esu/search client (collection-id->index collection-id) collection-id options)
          count-before-pagination (-> result :hits :total)
          aggregations (:aggregations result)
          meta (cond-> {:count count-before-pagination}
                       aggregations (assoc :aggregations aggregations))
          hits (->> result
                    :hits
                    :hits
                    (map :_source)
                    (map acl-utils/normalize-acl))]
      [meta hits]))

  Closeable
  (close [_]
    (try
      (.close client)
      (catch Exception _ nil)
      (finally nil))))



