(ns com.sixsq.slipstream.auth.sign
  (:require [buddy.sign.jws :as jws]
            [buddy.core.keys :as ks]
            [clojure.java.io :as io]
            [com.sixsq.slipstream.auth.utils.config :as cf]
            [buddy.core.codecs :as co]
            [buddy.core.hash :as ha]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]))

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
  [private-key-pem]
  (let [passphrase (cf/property-value :passphrase)
        privkey (io/resource private-key-pem)]
    (if (and passphrase privkey)
      (ks/private-key privkey passphrase)
      (throw (IllegalStateException. "Passphrase not defined or private key not accessible (must be in the classpath).")))))

(defn- public-key
  [public-key-pem]
  (let [pubkey (io/resource public-key-pem)]
    (if pubkey
      (ks/public-key pubkey)
      (throw (IllegalStateException. "Public key not accessible (must be in the classpath).")))))

(defn sign-claims
  [claims]
  (jws/sign claims (private-key "auth_privkey.pem") signing-algorithm))

(defn unsign-claims
  ([token]
   (unsign-claims token "auth_pubkey.pem"))
  ([token pubkey-pem]
   (log/info "will unsign token:" token) ;; TODO
   (jws/unsign token (public-key pubkey-pem) signing-algorithm)))
