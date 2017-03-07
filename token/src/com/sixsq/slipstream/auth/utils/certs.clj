(ns com.sixsq.slipstream.auth.utils.certs
  (:require
    [buddy.core.keys :as ks]
    [clojure.java.io :as io]
    [com.sixsq.slipstream.auth.utils.config :as cf]))

(def ^:const private-key-name "auth_privkey.pem")
(def ^:const public-key-name "auth_pubkey.pem")

(defn read-private-key
  [private-key-pem]
  (let [passphrase (cf/property-value :passphrase)
        privkey (io/resource private-key-pem)]
    (if (and passphrase privkey)
      (ks/private-key privkey passphrase)
      (throw (IllegalStateException. "Passphrase not defined or private key not accessible (must be in the classpath).")))))

(defn read-public-key
  [public-key-pem]
  (let [pubkey (io/resource public-key-pem)]
    (if pubkey
      (ks/public-key pubkey)
      (throw (IllegalStateException. "Public key not accessible (must be in the classpath).")))))

(def public-key (delay (read-public-key public-key-name)))

(def private-key (delay (read-private-key private-key-name)))
