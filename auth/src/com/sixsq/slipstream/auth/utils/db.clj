(ns com.sixsq.slipstream.auth.utils.db
  (:import (java.util Date UUID))
  (:require
    [clojure.string :as s]
    [clojure.tools.logging :as log]
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

(def ^:private active-user ["NEW" "ACTIVE"])

(defn find-usernames-by-email
  [email]
  (init)
  (when email
    (map :NAME (kc/select users
                          (kc/fields [:NAME])
                          (kc/where {:EMAIL email
                                     :STATE [in active-user]})))))

(defn- column-name
  [authn-method]
  (-> authn-method
      name
      (str "login")
      s/upper-case))

(defn- column-keyword
  [authn-method]
  (keyword (column-name authn-method)))


(defn find-username-by-authn
  [authn-method authn-id]
  (init)
  (when (and authn-method authn-id)
    (let [matched-users
          (kc/select users
                     (kc/fields [:NAME])
                     (kc/where {(column-keyword authn-method) authn-id
                                :STATE                        [in active-user]}))]
      (if (> (count matched-users) 1)
        (throw (Exception. (str "There should be only one result for " authn-id)))
        (:NAME (first matched-users))))))

(defn user-exists?
  "Verifies that a user with the given username exists in the database and
   that the account is active."
  [username]
  (init)
  (when username
    (let [matched-users (kc/select users
                                   (kc/fields [:NAME])
                                   (kc/where {:NAME  username
                                              :STATE [in active-user]}))]
      (pos? (count matched-users)))))

(defn update-user-authn-info
  [authn-method slipstream-username authn-id]
  (init)
  (kc/update users
             (kc/set-fields {(column-keyword authn-method) authn-id})
             (kc/where {:NAME slipstream-username}))
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
  (map :NAME (kc/select users (kc/fields [:NAME]))))

(defn random-password
  []
  (str (UUID/randomUUID)))

(defn create-user!
  "Create a new user in the database. Values for 'email' and 'authn-login'
   must be provided. NOTE: The 'authn-login' value may be modified to avoid
   collisions with existing users. The value used to create the account is
   returned."
  ([{:keys [authn-login email authn-method firstname lastname roles organization state fail-on-existing?]}]
   (init)
   (let [slipstream-username (name-no-collision authn-login (existing-user-names))
         user-record (cond-> {"RESOURCEURI" (str "user/" slipstream-username)
                              "DELETED"     false
                              "JPAVERSION"  0
                              "ISSUPERUSER" false
                              "STATE"       (or state "ACTIVE")
                              "NAME"        slipstream-username
                              "PASSWORD"    (random-password)
                              "CREATION"    (Date.)}
                             firstname (assoc "FIRSTNAME" firstname)
                             lastname (assoc "LASTNAME" lastname)
                             email (assoc "EMAIL" email)
                             roles (assoc "ROLES" roles)
                             organization (assoc "ORGANIZATION" organization)
                             authn-method (assoc (column-name authn-method) authn-login))]
     (when (or (not fail-on-existing?) (= authn-login slipstream-username))
       (kc/insert users (kc/values user-record))
       slipstream-username)))
  ([authn-method authn-login email]
   (create-user! {:authn-login  authn-login
                  :authn-method authn-method
                  :email        email}))
  ([authn-login email]
   (create-user! {:authn-login authn-login
                  :email       email})))

(defn find-password-for-username
  [username]
  (init)
  (-> (kc/select users
                 (kc/fields :PASSWORD)
                 (kc/where {:NAME  username
                            :STATE [in active-user]}))
      first
      :PASSWORD))

(defn build-roles [super? roles-string]
  (let [initial-role (if super? ["ADMIN" "USER" "ANON"] ["USER" "ANON"])
        roles (->> (s/split (or roles-string "") #"[\s,]+")
                   (remove nil?)
                   (remove s/blank?))]
    (s/join " " (concat initial-role roles))))

(defn find-roles-for-username
  [username]
  (init)
  (let [user-entry (first (kc/select users
                                     (kc/fields :ROLES :ISSUPERUSER)
                                     (kc/where {:NAME  username
                                                :STATE [in active-user]})))]
    (build-roles (:ISSUPERUSER user-entry) (:ROLES user-entry))))
