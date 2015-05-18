(ns com.sixsq.slipstream.ssclj.db.filesystem-binding
  (:require
    [fs.core :as fs]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.db.binding :refer [Binding]]
    [com.sixsq.slipstream.ssclj.db.filesystem-binding-utils :refer :all]
    [ring.util.response :as r]))

(def default-db-prefix "testdb/")

(deftype FilesystemBinding [db-prefix]
  Binding

  (add
    [this collection-id {:keys [id] :as data}]
    (let [fname (str db-prefix id)]
      (if-not (fs/exists? fname)
        (do
          (serialize-file fname data)
          (-> (str "created " id)
              (u/map-response 201 id)
              (r/header "Location" id)))
        (throw (u/ex-conflict id)))))

  (retrieve
    [this id]
    (let [fname (str db-prefix id)]
      (if (fs/exists? fname)
        (deserialize-file fname)
        (throw (u/ex-not-found id)))))

  (edit
    [this {:keys [id] :as data}]
    (let [fname (str db-prefix id)]
      (if (fs/exists? fname)
        (serialize-file fname data)
        (throw (u/ex-not-found id)))))

  (delete
    [this {:keys [id] :as data}]
    (let [fname (str db-prefix id)]
      (if (fs/exists? fname)
        (do
          (fs/delete fname)
          (-> (str id " deleted")
              (u/map-response 204 id)))
        (throw (u/ex-not-found id)))))

  (query
    [this collection-id options]
    (let [dname (str db-prefix collection-id)]
      (if (fs/directory? dname)
        (map deserialize-file (map #(str dname "/" %) (fs/list-dir dname)))
        (-> (str collection-id " isn't a collection")
            (u/ex-response 400 collection-id)
            (throw)))))

  )

(defn get-instance
  [db-prefix]
  (FilesystemBinding. db-prefix))
