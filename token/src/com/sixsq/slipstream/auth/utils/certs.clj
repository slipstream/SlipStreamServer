(ns com.sixsq.slipstream.auth.utils.certs
  (:require
    [buddy.core.keys :as ks]
    [clojure.tools.logging :as log]
    [environ.core :as environ]))

(def ^:const default-public-key-path "/etc/slipstream/auth/auth_pubkey.pem")
(def ^:const default-private-key-path "/etc/slipstream/auth/auth_privkey.pem")

(defn key-path
  [key-env-var default-path]
  (let [path (environ/env key-env-var default-path)]
    (log/info "Using key path" path "for" (name key-env-var) "env. variable.")
    path))

(defn read-public-key
  [key-env-var]
  (try
    (ks/public-key (key-path key-env-var default-public-key-path))
    (catch Exception e
      (log/error "Error reading public key:" (str e))
      (throw e))))

(defn read-private-key
  [key-env-var]
  (try
    (ks/private-key (key-path key-env-var default-private-key-path))
    (catch Exception e
      (log/error "Error reading private key:" (str e))
      (throw e))))

(def public-key (memoize read-public-key))

(def private-key (memoize read-private-key))
