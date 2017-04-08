(ns com.sixsq.slipstream.auth.test-helper
  (:refer-clojure :exclude [update])
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :refer :all :as jdbc]
    [clojure.string :as str]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [korma.core :as kc]
    [com.sixsq.slipstream.auth.internal :as ia]))

(defn- simple-surrounder
  [c]
  (fn sur [s]
    (str c s c)))

(defn- surrounder
  [c]
  (fn [s]
    (->> (str/split s #"\.")
         (map (simple-surrounder c))
         (str/join "."))))

(def double-quote (surrounder \"))

(defn- column-description
  [[name type]]
  (str (double-quote name) " " type))

(defn- columns
  [& name-types]
  (->> name-types
       (partition 2)
       (map column-description)
       (str/join ",")))

(defonce ^:private columns-users (columns "NAME" "VARCHAR(100)"
                                          "PASSWORD" "VARCHAR(200)"
                                          "EMAIL" "VARCHAR(200)"
                                          "GITHUBLOGIN" "VARCHAR(200)"
                                          "CYCLONELOGIN" "VARCHAR(200)"
                                          "CREATION" "TIMESTAMP"
                                          "DELETED" "BOOLEAN"
                                          "ISSUPERUSER" "BOOLEAN"
                                          "ROLES" "VARCHAR(255)"
                                          "RESOURCEURI" "VARCHAR(200)"
                                          "JPAVERSION" "INTEGER"
                                          "STATE" "VARCHAR(200)"
                                          ))

(defn- create-table!
  [table columns & [options]]
  (jdbc/execute! (db/db-spec) [(str "CREATE TABLE IF NOT EXISTS " (double-quote table) " ( " columns options " ) ")])
  (log/info "Created (if needed!) table:" table ", columns:" columns ", options: " options))

(defn create-test-empty-user-table
  []
  (db/init)
  (create-table! "USER" columns-users)
  (kc/delete db/users))

(defn add-user-for-test!
  [user]
  (db/init)
  (kc/insert db/users (kc/values {:NAME        (:username user)
                                  :PASSWORD    (ia/hash-password (:password user))
                                  :EMAIL       (:email user)
                                  :GITHUBLOGIN (:github-id user)
                                  :STATE       (or (:state user) "ACTIVE")
                                  :ISSUPERUSER (boolean (:issuperuser user))})))
