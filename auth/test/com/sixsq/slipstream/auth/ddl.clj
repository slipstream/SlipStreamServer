(ns com.sixsq.slipstream.auth.ddl
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :refer :all :as jdbc]
    [clojure.string :refer [join split]]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [korma.core :as kc]))


(defn simple-surrounder
  [c]
  (fn sur [s]
    (str c s c)))

(defn surrounder
  [c]
  (fn [s]
    (->>  (split s #"\.")
          (map (simple-surrounder c))
          (join "."))))

(def double-quote (surrounder \"))

(defn- column-description
  [[name type]]
  (str (double-quote name) " " type))

(defn columns
  [& name-types]
  (->>  name-types
        (partition 2)
        (map column-description)
        (join ",")))

(defonce ^:private columns-users (columns "NAME"        "VARCHAR(100)"
                                          "PASSWORD"    "VARCHAR(200)"
                                          "EMAIL"       "VARCHAR(200)"
                                          "AUTHNMETHOD" "VARCHAR(200)"
                                          "AUTHNID"     "VARCHAR(200)"))

(defn create-table!
  [table columns & [options]]
  (jdbc/execute! (db/db-spec) [(str "CREATE TABLE IF NOT EXISTS " (double-quote table) " ( " columns options " ) ")])
  (log/info "Created (if needed!) table:" table ", columns:" columns ", options: "options))

(defn create-fake-empty-user-table
  []
  (db/init)
  (create-table! "USER" columns-users)
  (kc/delete db/users))