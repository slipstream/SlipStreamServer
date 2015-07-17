(ns com.sixsq.slipstream.auth.simple-authentication
  (:refer-clojure :exclude [update])
  (:require

    [clojure.tools.logging                            :as log]

    [korma.core                                       :as kc]
    [com.sixsq.slipstream.auth.core                   :as core]
    [com.sixsq.slipstream.auth.database.korma-helper  :as kh]
    [com.sixsq.slipstream.auth.database.ddl           :as ddl]

    [buddy.hashers                                    :as hs]
    [buddy.sign.jws                                   :as jws]
    [buddy.core.keys                                  :as ks]
    [clj-time.core                                    :as t]
    [clojure.java.io                                  :as io]

    ))

;; TODO
(def passphrase "b8ddy-pr0t0")

(def unique-user-name
  (str ", UNIQUE (" (ddl/double-quote-list ["user_name"])")"))

(defonce ^:private columns-users
  (ddl/columns
    "user_name"            "VARCHAR(100)"
    "encrypted_password"   "VARCHAR(200)"))

(def init-db
  (delay
    (kh/korma-init)
    (log/info "Korma init done")

    (ddl/create-table! "users"    columns-users unique-user-name)
    (ddl/create-index! "users"   "IDX_USERS" "user_name")

    (kc/defentity users)
    (kc/select users (kc/limit 1))

    (log/info "Korma Entities defined")))

(defn pkey [auth-conf]
  (ks/private-key
    (io/resource (:privkey auth-conf))
    (:passphrase auth-conf)))

(defn init
  []
  @init-db)

(defn auth-user-impl
  [credentials]
  (init)
  (let [user-name (:user-name credentials)
        password-credential (:password credentials)
        encrypted-in-db (-> (kc/select users
                                       (kc/fields [:encrypted_password])
                                       (kc/where {:user_name user-name}))
                            first
                            :encrypted_password)
        auth-ok (hs/check password-credential encrypted-in-db)]
    (if auth-ok
      [true (dissoc credentials :password)]
      [false {:message "Invalid username or password"}])
    ))

(def timestamp-next-day
  (clj-time.format/unparse (:date-time clj-time.format/formatters)
                           (t/plus (t/now) (t/days 1))))

(defn create-auth-token [auth-conf credentials]
  (let [[ok? res] (auth-user-impl credentials)]
    (if ok?
      [true {:token (jws/sign res
                              (pkey auth-conf)
                              {:alg :rs256 :exp timestamp-next-day})}]
      [false res])))

(deftype SimpleAuthentication
  []
  core/AuthenticationServer

  (add-user!
    [this user]
    (init)
    (log/info "Will add user " (:user-name user))
    (kc/insert users (kc/values { :user_name           (:user-name user)
                                  :encrypted_password  (hs/encrypt (:password user))})))

  (auth-user
    [this credentials]
    (auth-user-impl credentials)))

(defn get-instance
  []
  (SimpleAuthentication. ))