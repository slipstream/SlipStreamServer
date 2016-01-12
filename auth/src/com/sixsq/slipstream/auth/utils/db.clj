(ns com.sixsq.slipstream.auth.utils.db
  (:import (java.util Date UUID))
  (:require [clojure.tools.logging :as log]
            [korma.core :as kc]
            [korma.db :refer [defdb]]
            [com.sixsq.slipstream.auth.utils.config :as cf]))

(defn db-spec
  []
  (cf/property-value :auth-db))

(def init-db
  (delay
    (let [current-db-spec (db-spec)]
      (log/info (format "Creating korma database %s" current-db-spec))
      (defdb korma-auth-db current-db-spec))
      (log/info "Korma init done")
      (kc/defentity users (kc/table "USER") (kc/database korma-auth-db))
      (log/info "Korma Entities defined")))

(defn init
  []
  @init-db)

(defn find-usernames-by-email
  [email]
  (init)
  (->> (kc/select users
                  (kc/fields [:NAME])
                  (kc/where {:EMAIL email}))
       (map :NAME)))

(defn find-username-by-authn
  [authn-id]
  (init)
  (let [matched-users
        (kc/select users
                   (kc/fields [:NAME])
                   (kc/where {:GITHUBLOGIN authn-id}))]
    (if (> (count matched-users) 1)
      (throw (Exception. (str "There should be only one result for " authn-id)))
      (-> (first matched-users)
          :NAME))))

(defn update-user-authn-info
  [slipstream-username authn-id]
  (init)
  (kc/update users
             (kc/set-fields {:GITHUBLOGIN authn-id})
             (kc/where      {:NAME slipstream-username}))
  slipstream-username)

(defn- inc-string
  [s]
  (if (empty? s)
    1
    (inc (read-string s))))

(defn- append-number-or-inc
  [name]
  (if-let [tokens (re-find #"(\w*)(_)(\d*)" name)]
    (str (nth tokens 1) "_" (inc-string (nth tokens 3)))
    (str name "_1")))

(defn name-no-collision
  [name existing-names]
  (if ((set existing-names) name)
    (recur (append-number-or-inc name) existing-names)
    name))

(defn- existing-user-names
  []
  (->> (kc/select users (kc/fields [:NAME]))
       (map :NAME)))

(defn random-password
  []
  (str (UUID/randomUUID)))

(defn create-user!
  [authn-method authn-login email]
  (init)
  (let [slipstream-user-name (name-no-collision authn-login (existing-user-names))]
    (kc/insert users (kc/values {"RESOURCEURI" (str "user/" slipstream-user-name)
                                 "DELETED"     false
                                 "JPAVERSION"  0
                                 "ISSUPERUSER" false
                                 "STATE"       "ACTIVE"
                                 "NAME"        slipstream-user-name
                                 "PASSWORD"    (random-password)
                                 "EMAIL"       email
                                 "GITHUBLOGIN" authn-login
                                 "CREATION"    (Date.)}))
    slipstream-user-name))

(defn find-password-for-user-name
  [user-name]
  (init)
  (-> (kc/select users
                 (kc/fields [:PASSWORD])
                 (kc/where {:NAME user-name}))
      first
      :PASSWORD))