(ns com.sixsq.slipstream.auth.sign
  (:require
    [clojure.string :as s]
    [clojure.tools.logging :as log]
    [buddy.core.keys :as ks]
    [buddy.sign.jwt :as jwt]
    [clj-time.core :as t]
    [environ.core :refer [env]]
    [com.sixsq.slipstream.auth.utils.config :as cf]))

(def default-nb-minutes-expiry (* 7 24 60))
(def signing-algorithm {:alg :rs256})
(def default-auth-public-file-loc "/etc/slipstream/auth/auth_pubkey.pem")
(def default-auth-private-file-loc "/etc/slipstream/auth/auth_privkey.pem")

(defn expiry-timestamp
  []
  (->> (cf/property-value :token-nb-minutes-expiry default-nb-minutes-expiry)
       (* 60000)
       t/millis
       (t/plus (t/now))))

(defn get-env
  [key]
  (env key))

(defn default-auth-file-loc
  [key-type]
  (case key-type
    :private default-auth-private-file-loc
    :public default-auth-public-file-loc))

(defn key-path-default
  [key-type key-env-var]
  (let [auth-file-loc (default-auth-file-loc key-type)]
    (log/warn (format "No env var %s defined pointing to %s auth key. Using "
                      "default %s." (name key-env-var) (name key-type)
                      auth-file-loc))
    auth-file-loc))

(defn key-path-from-env
  [key-env-var key-type]
  (if-let [key-path (get-env key-env-var)]
    key-path
    (key-path-default key-type key-env-var)))

(defn do-read-key
  [key-path key-type]
  {:pre [(contains? #{:public :private} key-type)]}
  (case key-type
    :private (ks/private-key key-path)
    :public (ks/public-key key-path)))

(defn read-key
  "key-type - :public or :private"
  [key-env-var key-type]
  (-> key-env-var
      (key-path-from-env key-type)
      (do-read-key key-type)))

(defn read-private-key
  [key-env-var]
  (read-key key-env-var :private))

(defn read-public-key
  [key-env-var]
  (read-key key-env-var :public))

(def public-key (memoize read-public-key))

(def private-key (memoize read-private-key))

(defn sign-claims
  [claims]
  (jwt/sign claims (private-key :auth-private-key) signing-algorithm))

(defn unsign-claims
  ([token]
   (unsign-claims token :auth-public-key))
  ([token env-var-pubkey-path]
   (jwt/unsign token (public-key env-var-pubkey-path) signing-algorithm)))
