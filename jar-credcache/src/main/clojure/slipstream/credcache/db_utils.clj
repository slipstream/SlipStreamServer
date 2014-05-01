(ns slipstream.credcache.db-utils
  "Utilities for interacting with the Couchbase database for resource
   storage."
  (:require
    [clojure.tools.logging :as log]
    [couchbase-clj.client :as cbc]
    [couchbase-clj.query :as cbq]
    [slipstream.credcache.utils :as utils])
  (:import
    [java.net URI]
    [com.couchbase.client.protocol.views DesignDocument ViewDesign]))

(defonce ^:dynamic *cb-client* nil)

(def ^:const cb-timeout-ms 3000)

(def cb-client-defaults {:uris     [(URI/create "http://localhost:8091/pools")]
                         :bucket   "default"
                         :username ""
                         :password ""})

(def ^:const design-doc-name "credcache.0")

(def ^:const doc-id-view-name "doc-id")

(def ^:const doc-id-view
  "
function(doc, meta) {
  if (meta.type==\"json\" && meta.id) {
    emit(meta.id,null);
  }
}
")

(defn create-design-doc
  []
  (let [view-design (ViewDesign. "doc-id" doc-id-view)]
    (DesignDocument. design-doc-name [view-design] nil)))

(defn add-design-doc
  []
  (if *cb-client*
    (let [java-cb-client (cbc/get-client *cb-client*)]
      (try
        (.getDesignDocument java-cb-client design-doc-name)
        false
        (catch Exception e
          (log/warn "creating design document" design-doc-name "->" (str e))
          (->> (create-design-doc)
               (.createDesignDoc java-cb-client)))))))

(defn get-doc-id-view
  []
  (if *cb-client*
    (cbc/get-view *cb-client* design-doc-name doc-id-view-name)))

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
    (log/info "created couchbase client" *cb-client*)
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

(defn get-resource-ids-chunk
  "Returns a chunk of resource ids skipping 'skip' entries.  The chunk size is 100."
  [resource-type skip]
  (let [start-key (str resource-type "/")
        end-key (str start-key "\uefff")
        q (cbq/create-query {:include-docs false
                             :range-start  start-key
                             :range-end    end-key
                             :limit        100
                             :skip         skip
                             :stale        false
                             :on-error     :continue})
        v (get-doc-id-view)]
    (->> (cbc/query *cb-client* v q)
         (map cbc/view-id))))

(defn all-resource-ids
  "Returns a lazy sequence of all of the resource ids associated with the given
   resource type.  Internally this will chunk the queries to the underlying
   database."
  ([resource-type]
   (all-resource-ids resource-type 0))
  ([resource-type skip]
    (let [chunk (get-resource-ids-chunk resource-type skip)
          n (count chunk)]
      (if (pos? n)
        (concat chunk (lazy-seq (all-resource-ids resource-type (+ n skip))))))))
