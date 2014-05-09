(ns slipstream.credcache.db-utils
  "Utilities for interacting with the Couchbase database for resource
   storage."
  (:require
    [clojure.tools.logging :as log]
    [clojure.data.json :as json]
    [slipstream.credcache.utils :as utils]
    [schema.core :as s]
    [me.raynes.fs :as fs])
  (:import
    [java.net URI]))

(def ^:dynamic *cache-dir* "/opt/slipstream/server/credcache")

(defn set-cache-dir!
  "Set the cache directory used to store cached credentials to a
   non-default value.  This is normally intended only for testing."
  [dir]
  (alter-var-root #'*cache-dir* (constantly dir)))

(defn create-cache-dir
  []
  (fs/mkdirs *cache-dir*))

(defn credential-file
  "Creates the filename for the saved credential based on the id."
  [id]
  (str *cache-dir* "/" id))

(defn spit-json
  "Write the json document to the cache.  This takes a map of the
   information to write."
  [id json]
  (let [fname (credential-file id)]
    (->> (json/write-str json)
         (spit fname))))

(defn slurp-json
  "Read a json document from the cache.  Returns a clojure map of
   the credential."
  [id]
  (let [fname (credential-file id)]
    (-> (slurp fname)
        (json/read-str :key-fn keyword))))

(defn delete-json
  "Deletes the json document from the cache."
  [id]
  (let [fname (credential-file id)]
    (fs/delete fname)))

(defn create-resource
  "Creates a new resource within the cache using the :id key.
   Returns the resource that was passed in."
  [{:keys [id] :as resource}]
  (spit-json id resource)
  resource)

(defn retrieve-resource
  "Retrieves the document associated with the given id."
  [id]
  (slurp-json id))

(defn update-resource
  "Updates an existing resource within the cache using the :id key.
   Returns the resource that was passed in."
  [{:keys [id] :as resource}]
  (spit-json id resource)
  resource)

(defn delete-resource
  "Deletes the credential information associated with the given id.  Returns nil."
  [id]
  (delete-json id))

(defn all-resource-ids
  "Returns a lazy sequence of all of the resource ids associated with the given
   resource type.  Internally this will chunk the queries to the underlying
   database."
  [resource-type]
  (fs/find-files (fs/list-dir *cache-dir*) (re-pattern (str "^" resource-type ".*"))))
