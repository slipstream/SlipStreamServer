(ns com.sixsq.slipstream.auth.token-checker
  (:require
    [clojure.walk :as walk]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [clojure.tools.logging :as log])
  (:gen-class
    :name com.sixsq.slipstream.auth.TokenChecker
    :methods [#^{:static true} [claimsInToken [String] java.util.Map]]))

(defn -claimsInToken
  "Validates the given token and returns the embedded claims.  If the token
   is not valid, then a warning is logged and an empty claims map is returned.
   This method facilitates token checking from Java. For clojure, use the
   validation functions directly."
  [^String token]
  (try
    (log/debug "checking token: " token)
    (let [claims (-> token
                     sign/unsign-claims
                     walk/stringify-keys)]
      (log/debug "validated token: " token)
      claims)
    (catch Exception e
      (log/warn "invalid authentication token: " token ", cause" e)
      {})))
