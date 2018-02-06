(ns com.sixsq.slipstream.db.atom.binding
  "Binding protocol implemented for data storage within an atom (or any object
   that behaves like an atom).

   The data is stored within the atom as a map with the following structure:

     {
       :collection-id-1 {
                          \"uuid1\" { ... document ... }
                          \"uuid2\" { ... document ... }
                        }
       :collection-id-2 {
                          \"uuid3\" { ... document ... }
                          \"uuid4\" { ... document ... }
                        }
     }

     The collection IDs are keywords, the document identifiers (uuids) are
     strings, and the document is standard EDN data."
  (:require
    [com.sixsq.slipstream.db.utils.common :as cu]
    [com.sixsq.slipstream.util.response :as response]
    [com.sixsq.slipstream.db.binding :refer [Binding]])
  (:import
    (java.io Closeable)))


(defn atomic-retrieve [data-atom id]
  (when-let [path (cu/split-id-kw id)]
    (or (get-in @data-atom path)
        (response/response-not-found id))))


(defn atomic-add
  [db {:keys [id] :as data}]
  (if-let [path (cu/split-id-kw id)]
    (if (get-in db path)
      (throw (response/ex-conflict id))
      (assoc-in db path data))
    (throw (response/ex-bad-request "invalid document id"))))


(defn add-data [data-atom {:keys [id] :as data}]
  (try
    (swap! data-atom atomic-add data)
    (response/response-created id)
    (catch Exception e
      (ex-data e))))


(defn atomic-update
  [db {:keys [id] :as data}]
  (if-let [path (cu/split-id-kw id)]
    (if (get-in db path)
      (assoc-in db path data)
      (throw (response/ex-not-found id)))
    (throw (response/ex-bad-request "invalid document id"))))


(defn update-data [data-atom {:keys [id] :as data}]
  (try
    (swap! data-atom atomic-update data)
    (response/response-updated id)
    (catch Exception e
      (ex-data e))))


(defn atomic-delete
  [db {:keys [id] :as data}]
  (if-let [[collection-id doc-id :as path] (cu/split-id-kw id)]
    (if (get-in db path)
      (update-in db [collection-id] dissoc doc-id)
      (throw (response/ex-not-found id)))
    (throw (response/ex-bad-request "invalid document id"))))


(defn delete-data [data-atom {:keys [id] :as data}]
  (try
    (swap! data-atom atomic-delete data)
    (response/response-deleted id)
    (catch Exception e
      (ex-data e))))


(defn query-info
  [data-atom collection-id options]
  (let [collection-kw (keyword collection-id)
        hits (vals (collection-kw @data-atom))
        meta {:count (count hits)}]
    [meta hits]))


(deftype AtomBinding
  [data-atom]

  Binding
  (add [_ data options]
    (add-data data-atom data))


  (add [_ _ data options]
    (add-data data-atom data))


  (retrieve [_ id options]
    (atomic-retrieve data-atom id))


  (delete [_ data options]
    (delete-data data-atom data))


  (edit [_ data options]
    (update-data data-atom data))


  (query [_ collection-id options]
    (query-info data-atom collection-id options))


  Closeable
  (close [_]
    nil))


(defn get-instance
  []
  (AtomBinding. (atom {})))
