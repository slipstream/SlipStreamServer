(ns com.sixsq.slipstream.auth.sign
  (:require
    [buddy.sign.jwt :as jwt]
    [com.sixsq.slipstream.auth.utils.certs :as certs]))

(def signing-algorithm {:alg :rs256})

(defn sign-claims
  "Signs the provided claims (as a map) with the service's private key."
  [claims]
  (jwt/sign claims @certs/private-key signing-algorithm))

(defn unsign-claims
  "Unsigns the provided token with the service's public key. This will
   throw an exception if the token has expired or if the token wasn't
   signed by the correct key."
  [token]
  (jwt/unsign token @certs/public-key signing-algorithm))
