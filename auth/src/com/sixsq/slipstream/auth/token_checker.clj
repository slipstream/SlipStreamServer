(ns com.sixsq.slipstream.auth.token-checker
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.auth.sign :as sg]
    [clojure.tools.logging :as log])
  (:gen-class
    :name com.sixsq.slipstream.auth.TokenChecker
    :methods [#^{:static true} [claimsInToken [String] java.util.Map]]))

(defn -claimsInToken
  [^String token]
  (try
    (log/debug "Will check authentication token: " token)
    (-> token
        sg/unsign-claims
        clojure.walk/stringify-keys)
    (catch Exception e
      (do
        (log/warn "Invalid authentication token : " token ", cause" e)
        {}))))
