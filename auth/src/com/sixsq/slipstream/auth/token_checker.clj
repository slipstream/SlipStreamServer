(ns com.sixsq.slipstream.auth.token-checker
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.auth.simple-authentication  :as sa]
    [clojure.tools.logging                            :as log])
  (:gen-class
    :name com.sixsq.slipstream.auth.TokenChecker
    :methods [#^{:static true} [checkToken [String] boolean]]))

(defn -checkToken
  [^String token]
  (try
      (log/info "Will check token: " token)
      (sa/check-token-impl token)
      (log/info "Token successfully checked: " token)
      true
      (catch Exception _
        (do
          (log/warn "Invalid token : " token)
          false))))
