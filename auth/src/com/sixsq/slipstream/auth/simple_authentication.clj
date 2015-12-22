(ns com.sixsq.slipstream.auth.simple-authentication
  (:refer-clojure :exclude [update])
  (:require

    [clojure.tools.logging                            :as log]
    [clojure.set                                      :refer [rename-keys]]
    [korma.core                                       :as kc]
    [com.sixsq.slipstream.auth.core                   :as core]
    [com.sixsq.slipstream.auth.database.korma-helper  :as kh]
    [com.sixsq.slipstream.auth.conf.config            :as cf]

    [buddy.sign.jws                                   :as jws]
    [buddy.core.hash                                  :as ha]
    [buddy.core.codecs                                :as co]
    [buddy.core.keys                                  :as ks]

    [clj-time.core                                    :as t]
    [clojure.java.io                                  :as io]))

(def auth-conf {:pubkey     "auth_pubkey.pem"
                :privkey    "auth_privkey.pem"})
(def default-nb-minutes-expiry (* 7 24 60))
(def signing-algorithm {:alg :rs256})

;;
;; DB
;;

(def init-db
  (delay
    (kh/korma-init)
    (log/info "Korma init done")
    (kc/defentity users (kc/table "USER") (kc/database kh/korma-auth-db))
    (log/info "Korma Entities defined")))
;;
;; DB
;;

(defn private-key
  [auth-conf]
  (let [passphrase (cf/property-value :passphrase)
        privkey    (io/resource (:privkey auth-conf))]
    (if (and passphrase privkey)
      (ks/private-key privkey passphrase)
      (throw (IllegalStateException. "Passphrase not defined or private key not accessible (must be in the classpath).")))))

(defn public-key
  [auth-conf]
  (let [pubkey (io/resource (:pubkey auth-conf))]
    (if pubkey
      (ks/public-key pubkey)
      (throw (IllegalStateException. "Public key not accessible (must be in the classpath).")))))

(defn init
  []
  @init-db)

(defn sha512
  "Encrypt secret exactly as done in SlipStream Java server."
  [secret]
  (-> (ha/sha512 secret)
      co/bytes->hex
      clojure.string/upper-case))

;; TODO : check user not already present, password rules (length, complexity...)
;; TODO : Currently unused as DB insertion is done by Java server
(defn add-user-impl
  [user]
  (init)
  (log/info "Will add user " (:user-name user))
  (kc/insert users (kc/values { :NAME         (:user-name user)
                                :PASSWORD     (sha512 (:password user))
                                :EMAIL        (:email user)
                                :AUTHNMETHOD  (:authn-method user)
                                :AUTHNID      (:authn-id user)})))

(defn auth-user-impl
  [credentials]
  (init)
  (let [user-name           (:user-name credentials)
        password-credential (:password credentials)
        encrypted-in-db     (-> (kc/select users
                                            (kc/fields [:PASSWORD])
                                            (kc/where {:NAME user-name}))
                                first
                                :PASSWORD)
        auth-ok (and
                  password-credential
                  encrypted-in-db
                  (= (sha512 password-credential) encrypted-in-db))]

    (if auth-ok
      [true (dissoc credentials :password)]
      [false {:message (str "Invalid combination username/password for '" user-name "'")}])))

(defn expiry-timestamp
  []
  (->> (cf/property-value :token-nb-minutes-expiry default-nb-minutes-expiry)
       (* 60000)
       t/millis
       (t/plus (t/now))))

(defn enrich-claims
  [claims]
  (-> claims
      (rename-keys {:user-name :com.sixsq.identifier})
      (merge {:exp (expiry-timestamp)})))

(defn check-token-impl
  [token]
  (log/info "will unsign token:" token)
  (jws/unsign token (public-key auth-conf) signing-algorithm))

(defn sign-claims
  [claims]
  (jws/sign claims (private-key auth-conf) signing-algorithm))

(defn create-token
  ([credentials]
  (let [[ok? claims] (auth-user-impl credentials)]
    (if ok?
      [true {:token (sign-claims (enrich-claims claims))}]
      [false {:message "Invalid credentials when creating token"}])))

  ([claims token]
   (log/info "Will create token for claims=" claims)
   (try
      (check-token-impl token)
      [true {:token (sign-claims claims)}]
      (catch Exception e
        (log/error "exception in token creation " e)
        [false {:message (str "Invalid token when creating token: " e)}]))))

(defn map-slipstream-github
  [slipstream-username github-login]
  (->> (kc/update users
                  (kc/set-fields (zipmap [:AUTHNMETHOD :AUTHNID] ["github" github-login]))
                  (kc/where {:NAME slipstream-username}))
       (map :NAME)))

(defn find-usernames-by-email
  [email]
  (->> (kc/select users
                  (kc/fields [:NAME])
                  (kc/where {:EMAIL email}))
       (map :NAME)))

(defn find-username-by-authn
  [authn-method authn-id]
  (let [matched-users
        (kc/select users
                  (kc/fields [:NAME])
                  (kc/where (zipmap [:AUTHNMETHOD :AUTHNID] [authn-method authn-id])))]
    (if (> (count matched-users) 1)
      (throw (Exception. (str "There should be only one result for " authn-method "/" authn-id)))
      (-> (first matched-users)
          :NAME))))

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
      (create-token credentials))

  (token
    [this claims token]
      (create-token claims token))

  (check-token
    [this token]
    (check-token-impl token)))

(defn get-instance
  []
  (SimpleAuthentication. ))