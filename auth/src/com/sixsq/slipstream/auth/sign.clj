(ns com.sixsq.slipstream.auth.sign
  (:require [buddy.sign.jws :as jws]
            [buddy.core.keys :as ks]
            [clojure.java.io :as io]
            [com.sixsq.slipstream.auth.conf.config :as cf]
            [buddy.core.codecs :as co]
            [buddy.core.hash :as ha]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]))

(def auth-conf {:pubkey     "auth_pubkey.pem"
                :privkey    "auth_privkey.pem"})
(def default-nb-minutes-expiry (* 7 24 60))
(def signing-algorithm {:alg :rs256})

(defn expiry-timestamp
  []
  (->> (cf/property-value :token-nb-minutes-expiry default-nb-minutes-expiry)
       (* 60000)
       t/millis
       (t/plus (t/now))))

(defn sha512
  "Encrypt secret exactly as done in SlipStream Java server."
  [secret]
  (-> (ha/sha512 secret)
      co/bytes->hex
      clojure.string/upper-case))

(defn private-key
  [auth-conf]
  (let [passphrase (cf/property-value :passphrase)
        privkey (io/resource (:privkey auth-conf))]
    (if (and passphrase privkey)
      (ks/private-key privkey passphrase)
      (throw (IllegalStateException. "Passphrase not defined or private key not accessible (must be in the classpath).")))))

(defn public-key
  [auth-conf]
  (let [pubkey (io/resource (:pubkey auth-conf))]
    (if pubkey
      (ks/public-key pubkey)
      (throw (IllegalStateException. "Public key not accessible (must be in the classpath).")))))

(defn sign-claims
  [claims]
  (jws/sign claims (private-key auth-conf) signing-algorithm))

(defn check-token
  [token]
  (log/debug "will unsign token:" token)
  (jws/unsign token (public-key auth-conf) signing-algorithm))
