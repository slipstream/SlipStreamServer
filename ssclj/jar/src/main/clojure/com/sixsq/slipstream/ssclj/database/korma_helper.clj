(ns com.sixsq.slipstream.ssclj.database.korma-helper
  (:require
    [korma.db :refer [defdb]]
    [clojure.tools.logging :as log]
    [environ.core :as environ]
    [clojure.edn :as edn]
    [superstring.core :as s]
    [clojure.java.io :as io]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def default-db-spec
  {:classname   "org.hsqldb.jdbc.JDBCDriver"
   :subprotocol "hsqldb"
   :subname     "mem://localhost:9012/devresources"
   :make-pool?  true})

(defn- find-resource
  [resource-path]
  (if-let [config-file (io/resource resource-path)]
    (do
      (log/info "================================================")
      (log/info "Will use " (.getPath config-file) " as config file")
      config-file)
    (let [msg (str "Resource not found (must be in classpath): '" resource-path "'")]
      (log/error msg)
      (throw (IllegalArgumentException. msg)))))

(def db-spec
  (if-let [config-path (environ/env :config-path)]
    (-> config-path
        find-resource
        slurp
        edn/read-string
        :api-db)
    (do
      (log/warn "Using default db spec: " default-db-spec)
      default-db-spec)))

(def korma-init-done
  (delay
    (log/info (format "Will create korma database with db-spec"))
    (log/info (u/map-multi-line db-spec))
    (defdb korma-api-db db-spec)
    (log/info "Korma database created")))

(defn korma-init
  "Initializes korma database"
  []
  @korma-init-done)
