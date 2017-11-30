(ns com.sixsq.slipstream.auth.utils.db
  (:import (java.util Date UUID)
           (clojure.lang ExceptionInfo))
  (:require
    [clojure.string :as s]
    [clojure.tools.logging :as log]
    [korma.core :as kc]
    [korma.db :refer [defdb]]
    [com.sixsq.slipstream.auth.utils.config :as cf]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    ;[com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    ))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (if (some #(= elm %) coll) true false))

(def user-resource-uri "user/")

(defn get-user
  [username]
  (try
    (db/retrieve (str "user/" username) {})
    (catch ExceptionInfo e {})))

(def ^:private active-user ["NEW" "ACTIVE"])
(def ^:private active-user-filter "(state='NEW' or state='ACTIVE')")

(defn find-usernames-by-email
  [email]
  (when email
    #_(map :NAME (kc/select users
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
  (when (and authn-method authn-id)
    (let [matched-users ()
          #_(kc/select users
                       (kc/fields [:NAME])
                       (kc/where {(column-keyword authn-method) authn-id
                                  :STATE                        [in active-user]}))]
      (if (> (count matched-users) 1)
        (throw (Exception. (str "There should be only one result for " authn-id)))
        (:NAME (first matched-users))))))

(defn get-active-user-by-name
  [username]
  (when username
    (let [filter-str (format "username='%s' and %s" username active-user-filter)
          filter     {:filter (parser/parse-cimi-filter filter-str)}]
      (try (-> (db/query "user" {:cimi-params filter
                                 :user-roles  ["ADMIN"]})
               second
               first)
           (catch ExceptionInfo e {})))))

(defn user-exists?
  "Verifies that a user with the given username exists in the database and
   that the account is active."
  [username]
  (in? active-user (:state (get-active-user-by-name username))))

(defn update-user-authn-info
  [authn-method slipstream-username authn-id]
  #_(kc/update users
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
  (map :NAME () #_(kc/select users (kc/fields [:NAME]))))

(defn random-password
  []
  (str (UUID/randomUUID)))

(defn create-user!
  "Create a new user in the database. Values for 'email' and 'authn-login'
   must be provided. NOTE: The 'authn-login' value may be modified to avoid
   collisions with existing users. The value used to create the account is
   returned."
  ([{:keys [authn-login email authn-method firstname lastname roles organization state fail-on-existing?]}]
   (let [slipstream-username (name-no-collision authn-login (existing-user-names))
         user-record         (cond-> {"RESOURCEURI" (str "user/" slipstream-username)
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
       () #_(kc/insert users (kc/values user-record))
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
  (:password (get-active-user-by-name username)))

(defn build-roles [super? roles-string]
  (let [initial-role (if super? ["ADMIN" "USER" "ANON"] ["USER" "ANON"])
        roles        (->> (s/split (or roles-string "") #"[\s,]+")
                          (remove nil?)
                          (remove s/blank?))]
    (s/join " " (concat initial-role roles))))

(defn find-roles-for-username
  [username]
  (let [user-entry (get-active-user-by-name username)]
    (build-roles (:isSuperUser user-entry) (:roles user-entry))))
