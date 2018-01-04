(ns com.sixsq.slipstream.auth.utils.sign
  (:require
    [buddy.sign.jwt :as jwt]
    [com.sixsq.slipstream.auth.utils.certs :as certs]))

(def signing-algorithm {:alg :rs256})

(defn sign-claims
  [claims]
  (jwt/sign claims (certs/private-key :auth-private-key) signing-algorithm))

(defn unsign-claims
  "If passed only the token from which to extract the claims, then the public
   key in the path defined by the keyword :auth-public-key will be used to
   verify the claims. If a second keyword argument is provided, it will be used
   to find the public key path. If the second argument is a string, it is
   treated as a raw RSA key string and will be used directly."
  ([token]
   (unsign-claims token :auth-public-key))
  ([token env-var-pubkey-path]
   (if (keyword? env-var-pubkey-path)
     (jwt/unsign token (certs/public-key env-var-pubkey-path) signing-algorithm)
     (jwt/unsign token (certs/str->public-key env-var-pubkey-path) signing-algorithm))))
