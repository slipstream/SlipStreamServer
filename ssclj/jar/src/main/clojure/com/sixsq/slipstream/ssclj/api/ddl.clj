(ns com.sixsq.slipstream.ssclj.api.ddl
  (:require    
    [clojure.java.jdbc :refer :all                    :as jdbc]
    [clojure.tools.logging                            :as log]
    [com.sixsq.slipstream.ssclj.database.ddl          :as ddlh]
    [com.sixsq.slipstream.ssclj.database.korma-helper :as kh]))

(def columns-acl   
  (ddlh/columns 
    "resource_id"     "VARCHAR(100)" 
    "resource_type"   "VARCHAR(50)" 
    "principal_type"  "VARCHAR(20)" 
    "principal_name"  "VARCHAR(100)"))

(def unique-acl
  (str ", UNIQUE (" (ddlh/double-quote-list ["resource_id" "resource_type" "principal_type" "principal_name"])")"))

(defn create-ddl
  []
  (jdbc/execute! kh/db-spec [(str "CREATE TABLE IF NOT EXISTS \"acl\" (" columns-acl unique-acl ")")])
  (ddlh/create-index! "acl" "IDX_TYPE_PRINCIPAL" "resource_type", "principal_type", "principal_name")
  (log/info "DDL created"))
