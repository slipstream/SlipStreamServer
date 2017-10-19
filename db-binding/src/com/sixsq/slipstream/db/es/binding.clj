;;
;; Elastic Search implementation of Binding protocol
;;
(ns com.sixsq.slipstream.db.es.binding
  (:require
    [ring.util.response :as r]
    [clojure.string :as str]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.es.acl :as acl]
    [com.sixsq.slipstream.db.binding :refer [Binding]])
  (:import (org.elasticsearch.index.engine VersionConflictEngineException)
           (clojure.lang ExceptionInfo)))

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

(defn- split-id
  "Split id in [type docid].
  id is usually in the form type/docid.
  Exception for cloud-entry-point: in this case id is only type (there is only one cloud-entry-point)"
  [id]
  (let [[type docid] (str/split id #"/")]
    [type (or docid type)]))

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
        uuid (second (split-id id))
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
        (catch VersionConflictEngineException e
          (response-conflict id)))))


  (retrieve [_ id options]
    (find-data *client* index-name id options "VIEW"))

  (delete [_ {:keys [id]} options]
    (find-data *client* index-name id options "MODIFY")
    (let [[type docid] (split-id id)]
      (.status (esu/delete *client* index-name type docid)))
    (response-deleted id))

  (edit [_ {:keys [id] :as data} options]
    (find-data *client* index-name id options "MODIFY")
    (let [[type docid] (split-id id)
          updated-doc (prepare-data data)]
      (if (esu/update *client* index-name type docid updated-doc)
        (cu/json-response data)                             ;; FIXME: return updated data from database?
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
