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
    (log/info "using key path" path "for" (name key-env-var) "env. variable.")
    path))

(defn read-key
  [read-key-fn default-path key-env-var]
  (try
    (read-key-fn (key-path key-env-var default-path))
    (catch Exception e
      (log/error "error reading key:" (str e))
      (throw e))))

(def public-key (memoize (partial read-key ks/public-key default-public-key-path)))

(def private-key (memoize (partial read-key ks/private-key default-private-key-path)))
