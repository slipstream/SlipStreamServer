(ns com.sixsq.slipstream.auth.token-checker
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.auth.simple-authentication :as sa]
    [clojure.tools.logging :as log])
  (:gen-class
    :name com.sixsq.slipstream.auth.TokenChecker
    :methods [#^{:static true} [claimsInToken [String] java.util.Map]]))

(defn -claimsInToken
  [^String token]
  (try
      (log/debug "Will check authentication token: " token)
      (-> token
          sa/check-token-impl
          clojure.walk/stringify-keys)
      (catch Exception e
        (do
          (log/warn "Invalid authentication token : " token ", cause" e)
          {}))))
