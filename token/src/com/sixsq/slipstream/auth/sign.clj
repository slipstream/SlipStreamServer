(ns com.sixsq.slipstream.auth.sign
  (:require
    [buddy.sign.jwt :as jwt]
    [com.sixsq.slipstream.auth.utils.certs :as certs]))

(def signing-algorithm {:alg :rs256})

(defn sign-claims
  [claims]
  (jwt/sign claims (certs/private-key :auth-private-key) signing-algorithm))

(defn unsign-claims
  ([token]
   (unsign-claims token :auth-public-key))
  ([token env-var-pubkey-path]
   (jwt/unsign token (certs/public-key env-var-pubkey-path) signing-algorithm)))
