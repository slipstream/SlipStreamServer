(ns com.sixsq.slipstream.ssclj.api.ddl
  (:require    
    [clojure.java.jdbc :refer :all                :as jdbc]
    [clojure.tools.logging                        :as log]
    [com.sixsq.slipstream.ssclj.api.korma-helper  :as kh]))

(def create-table-acl
  "CREATE TABLE IF NOT EXISTS \"acl\"
  (\"resource-id\" VARCHAR(100), \"resource-type\" VARCHAR(50), \"principal-type\" VARCHAR(20), \"principal-name\" VARCHAR(100),
  UNIQUE (\"resource-id\", \"resource-type\", \"principal-type\", \"principal-name\"))")

(defn- quote [name] (str "\""name"\""))

(defn- quote-list 
  [names]
  (->> names
    (map quote)
    (clojure.string/join ",")))

(defn- create-index
  [index-name column-names]
  (jdbc/execute! kh/db-spec [(str "DROP INDEX IF EXISTS " index-name)])
  (jdbc/execute! kh/db-spec [(str "CREATE INDEX " index-name " ON \"acl\" (" (quote-list column-names) ")")]))

(defn create-ddl
  []
  (jdbc/execute! kh/db-spec [create-table-acl])
  (create-index "IDX_TYPE_PRINCIPAL" ["resource-type", "principal-type", "principal-name"])
  (log/info "DDL created"))
