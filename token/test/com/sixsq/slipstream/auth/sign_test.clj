(ns com.sixsq.slipstream.auth.sign-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [clojure.java.io :as io]
    [environ.core]
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.auth.sign :as t]))

(def env-authn {"AUTH_PRIVATE_KEY" (io/resource "auth_privkey.pem")
                "AUTH_PUBLIC_KEY"  (io/resource "auth_pubkey.pem")})
(def env-map (into {} (map (fn [[k v]] [(#'environ.core/keywordize k) v])
                           (seq env-authn))))

(deftest roundtrip-claims
  (let [claims {:alpha "alpha"
                :beta  2
                :gamma 3.0
                :delta true
                :exp   (ts/expiry-later)}]
    (with-redefs [environ.core/env env-map]
      (is (= claims (t/unsign-claims (t/sign-claims claims)))))))
