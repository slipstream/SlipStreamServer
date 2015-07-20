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

;; TODO do not show private information in source code.
(def auth-conf {:pubkey     "auth_pubkey.pem"
                :privkey    "auth_privkey.pem"
                :passphrase "b8ddy-pr0t0"})

;;
;; DB
;;
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
;;
;; DB
;;

(defn private-key
  [auth-conf]
  (ks/private-key
    (io/resource (:privkey auth-conf))
    (:passphrase auth-conf)))

(defn public-key
  [auth-conf]
  (ks/public-key (io/resource (:pubkey auth-conf))))

(defn init
  []
  @init-db)

(defn add-user-impl
  [user]
  ;; TODO : check user not already present, password rules (length, complexity...)
  (init)
  (log/info "Will add user " (:user-name user))
  (kc/insert users (kc/values { :user_name           (:user-name user)
                                :encrypted_password  (hs/encrypt (:password user))})))

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
  (t/plus (t/now) (t/days 1)))

(defn token-impl
  [credentials]
  (let [[ok? claims] (auth-user-impl credentials)]
    (if ok?
      [true {:token (jws/sign (merge claims {:exp timestamp-next-day})
                              (private-key auth-conf)
                              {:alg :rs256})}]
      [false {:message "Invalid username or password"}])))

(defn check-token-impl
  [token]
  (jws/unsign token (public-key auth-conf) {:alg :rs256}))

(deftype SimpleAuthentication
  []
  core/AuthenticationServer

  (add-user!
    [this user]
    (add-user-impl user))

  (auth-user
    [this credentials]
    (auth-user-impl credentials))

  (token
    [this credentials]
    (token-impl credentials))

  (check-token
    [this token]
    (check-token-impl token)))

(defn get-instance
  []
  (SimpleAuthentication. ))