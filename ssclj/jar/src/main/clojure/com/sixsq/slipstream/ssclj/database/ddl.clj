(ns com.sixsq.slipstream.ssclj.database.ddl
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :refer :all :as jdbc]
    [com.sixsq.slipstream.ssclj.database.korma-helper :as kh]))

(defn- double-quote 
  [s] 
  (str \" s \"))

(defn- column-description 
  [[name type]]
  (str (double-quote name) " " type))

(defn quote-list 
  [names]
  (->> names
    (map double-quote)
    (clojure.string/join ",")))

(defn columns 
  [& name-types] 
  (->> 
    name-types
    (partition 2)
    (map column-description)
    (clojure.string/join ",")))      

(defn create-table!   
  [table columns]  
  (jdbc/execute! kh/db-spec [(str "CREATE TABLE IF NOT EXISTS " (double-quote table)" (" columns ")")])
  (log/info "Created (if needed!) table:"table", columns:"columns))

(defn create-index!
  [table index-name & column-names]
  (jdbc/execute! kh/db-spec [(str "DROP INDEX IF EXISTS " index-name)])
  (jdbc/execute! kh/db-spec [(str "CREATE INDEX " index-name " ON \""table"\" (" (quote-list column-names) ")")]))
