(ns com.sixsq.slipstream.auth.database.ddl
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :refer :all :as jdbc]
    [clojure.string :refer [join split]]
    [com.sixsq.slipstream.auth.utils.db :as db]))

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

(defn create-table!
  [table columns & [options]]
  (jdbc/execute! (db/db-spec) [(str "CREATE TABLE IF NOT EXISTS " (double-quote table) " ( " columns options " ) ")])
  (log/info "Created (if needed!) table:" table ", columns:" columns ", options: "options))