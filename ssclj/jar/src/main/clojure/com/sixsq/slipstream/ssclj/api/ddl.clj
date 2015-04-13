(ns com.sixsq.slipstream.ssclj.api.ddl
  (:require    
    [clojure.java.jdbc :refer :all                :as jdbc]
    [clojure.tools.logging                        :as log]
    [com.sixsq.slipstream.ssclj.database.ddl      :as ddlh]
    [com.sixsq.slipstream.ssclj.database.korma-helper  :as kh]
    ))

(def columns-acl   
  (ddlh/columns 
    "resource-id"     "VARCHAR(100)" 
    "resource-type"   "VARCHAR(50)" 
    "principal-type"  "VARCHAR(20)" 
    "principal-name"  "VARCHAR(100)"))

(def unique-acl
  (str ", UNIQUE (" (ddlh/quote-list ["resource-id" "resource-type" "principal-type" "principal-name"])")"))

(defn create-ddl
  []
  (jdbc/execute! kh/db-spec [(str "CREATE TABLE IF NOT EXISTS \"acl\" (" columns-acl unique-acl ")")])
  (ddlh/create-index! "acl" "IDX_TYPE_PRINCIPAL" "resource-type", "principal-type", "principal-name")
  (log/info "DDL created"))
