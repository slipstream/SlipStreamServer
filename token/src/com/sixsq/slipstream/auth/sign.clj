(ns com.sixsq.slipstream.auth.sign
  (:require
    [buddy.core.keys :as ks]
    [buddy.sign.jwt :as jwt]
    [clj-time.core :as t]
    [clojure.java.io :as io]
    [clojure.string :as s]

    [environ :as env]

    [com.sixsq.slipstream.auth.utils.config :as cf]))

(def default-nb-minutes-expiry (* 7 24 60))
(def signing-algorithm {:alg :rs256})

(defn expiry-timestamp
  []
  (->> (cf/property-value :token-nb-minutes-expiry default-nb-minutes-expiry)
       (* 60000)
       t/millis
       (t/plus (t/now))))

(defn- read-private-key
  [private-key-pem]
  (if-let [privkey (io/resource private-key-pem)]
    (ks/private-key privkey)
    (throw (IllegalStateException. "Private key not accessible (must be in the classpath)."))))

(defn- read-public-key
  [public-key-pem]
  (let [pubkey (io/resource public-key-pem)]
    (if pubkey
      (ks/public-key pubkey)
      (throw (IllegalStateException. "Public key not accessible (must be in the classpath).")))))

(def public-key (memoize read-public-key))

(def private-key (memoize read-private-key))

(defn sign-claims
  [claims]
  (jwt/sign claims (private-key "auth_privkey.pem") signing-algorithm))

(defn unsign-claims
  ([token]
   (unsign-claims token (env/env :authn-public-key)))
  ([token pubkey-pem]
   (jwt/unsign token (public-key pubkey-pem) signing-algorithm)))
