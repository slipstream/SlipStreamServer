;;
;; In-memory implementation of Binding protocol based on an atom
;;
(ns com.sixsq.slipstream.db.mem.binding
  (:require
    [ring.util.response :as r]
    [clojure.string :as str]
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.db.utils.responses :as responses]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.es.acl :as acl]
    [com.sixsq.slipstream.db.binding :refer [Binding]]))


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


(defn- find-data
  [data-atom id options]
  (let [id-tuple (cu/split-id id)]
    (or (get-in @data-atom id-tuple)
        (throw (responses/ex-not-found id)))))


(defn- add-data
  [current]
  nil)


(defn- delete-data
  [current]
  nil)


(defn- edit-data
  [current updated]
  nil)


(deftype InMemoryBinding [data-atom]

  Binding
  (add [_ type {:keys [id] :as data} options]
    (swap! data-atom add-data data))


  (retrieve [_ id options]
    (find-data data-atom id options))


  (delete [_ {:keys [id]} options]
    (swap! data-atom delete-data))


  (edit [_ data options]
    (let [updated-doc (prepare-data data)]
      (swap! data-atom edit-data updated-doc)))


  (query [_ collection-id options]
    [{} {}]))


(defn get-instance
  []
  (InMemoryBinding. (atom {})))
