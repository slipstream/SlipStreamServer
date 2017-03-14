(ns com.sixsq.slipstream.auth.utils.certs-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [buddy.sign.util :as util]
    [clojure.java.io :as io]
    [environ.core]
    [com.sixsq.slipstream.auth.utils.certs :as t]))

(def env-authn {"AUTH_PRIVATE_KEY" (io/resource "auth_privkey.pem")
                "AUTH_PUBLIC_KEY"  (io/resource "auth_pubkey.pem")})
(def env-map (into {} (map (fn [[k v]] [(#'environ.core/keywordize k) v]) env-authn)))

(deftest test-key-path
  (with-redefs [environ.core/env {}]
    (is (= t/default-private-key-path
           (t/key-path :auth-private-key t/default-private-key-path)))
    (is (= t/default-public-key-path
           (t/key-path :auth-public-key t/default-public-key-path))))

  (with-redefs [environ.core/env env-map]
    (is (= (get env-authn "AUTH_PRIVATE_KEY")
           (t/key-path :auth-private-key t/default-private-key-path)))
    (is (= (get env-authn "AUTH_PUBLIC_KEY")
           (t/key-path :auth-public-key t/default-public-key-path)))))

(deftest check-read-key
  (with-redefs [environ.core/env env-map]
    (is (t/read-private-key :auth-private-key))
    (is (t/private-key :auth-private-key))
    (is (t/read-public-key :auth-public-key))
    (is (t/public-key :auth-public-key))))

(deftest check-throws-unknown-key
  (with-redefs [t/key-path (fn [_ _ ] "/unknown/key-path.pem")
                environ.core/env {}]
    (is (thrown? Exception (t/read-private-key :auth-private-key)))
    (is (thrown? Exception (t/read-public-key :auth-public-key)))))
