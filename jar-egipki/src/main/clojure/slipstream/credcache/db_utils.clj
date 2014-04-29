(ns slipstream.credcache.db-utils
  "Utilities for interacting with the Couchbase database for resource
   storage."
  (:require
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [slipstream.credcache.utils :as utils])
  (:import
    [java.net URI]))

(def ^:dynamic *cb-client* nil)

(def ^:const cb-timeout-ms 3000)

(def cb-client-defaults {:uris     [(URI/create "http://localhost:8091/pools")]
                         :bucket   "default"
                         :username ""
                         :password ""})

(defn create-client
  "Creates a Couchbase client for accessing resources in the database.  This
   is a shared, thread-safe instance stored in a dynamic variable.  This should
   be called before trying to use any other utilities in this namespace."
  []

  ;; force logging to use SLF4J facade
  (System/setProperty "net.spy.log.LoggerImpl" "net.spy.memcached.compat.log.SLF4JLogger")

  (try
    (->> (cbc/create-client cb-client-defaults)
         (constantly)
         (alter-var-root #'*cb-client*))

    (catch Exception e
      (log/error "error creating couchbase client" (str e))
      nil)))

(defn destroy-client
  "Shutdown and free all resources for the Couchbase client.  This should be
   called when shutting down the system to cleanly shutdown connections to
   the database."
  []
  (cbc/shutdown *cb-client* cb-timeout-ms)
  (->> (constantly nil)
       (alter-var-root #'*cb-client*)))

(defn create-resource
  "Creates a new resource within the database using the :id and :expiry keys. If the
   :expiry key is given (in milliseconds since the epoch), then the expiry value is
   set for the Couchbase document.  If not, then the document does not expire.
   Returns the resource that was passed in."
  [{:keys [id expiry] :or {:expiry -1} :as resource}]
  (let [opts {:expiry expiry}]
    (cbc/add-json *cb-client* id resource opts)
    resource))

(defn retrieve-resource
  "Retrieves the document associated with the given id."
  [id]
  (cbc/get-json *cb-client* id))

(defn update-resource
  "Updates an existing resource within the database using the :id and :expiry keys.
   If the :expiry key is given (in milliseconds since the epoch), then the expiry
   value is set for the Couchbase document.  If not, the document does not expire.
   Returns the resource that was passed in."
  [{:keys [id expiry] :or {:expiry -1} :as resource}]
  (let [opts (if expiry {:expiry expiry} {})]
    (cbc/set-json *cb-client* id resource opts)
    resource))

(defn delete-resource
  "Deletes the credential information associated with the given id.  Returns nil."
  [id]
  (cbc/delete *cb-client* id))
