(ns com.sixsq.slipstream.auth.sign-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [clojure.java.io :as io]
    [buddy.sign.util :as util]
    [environ.core]
    [com.sixsq.slipstream.auth.sign :as t]
    [clojure.java.io :as io]))

(def env-authn {"AUTH_PRIVATE_KEY" (io/resource "auth_privkey.pem")
                "AUTH_PUBLIC_KEY"  (io/resource "auth_pubkey.pem")})
(def env-map (into {} (map (fn [[k v]] [(#'environ.core/keywordize k) v])
                           (seq env-authn))))
(def redef-get-env {#'com.sixsq.slipstream.auth.sign/get-env
                    (fn [key] (key env-map))})
(def redef-get-env-empty {#'com.sixsq.slipstream.auth.sign/get-env
                          (fn [_] nil)})

(deftest roundtrip-claims
  (let [exp       (t/expiry-timestamp)
        timestamp (util/to-timestamp exp)
        claims    {:alpha "alpha"
                   :beta  2
                   :gamma 3.0
                   :delta true
                   :exp   exp}]
    (with-redefs-fn redef-get-env
      #(is (= (merge claims {:exp timestamp})
              (t/unsign-claims (t/sign-claims claims)))))))

(deftest test-key-path-from-env
  (with-redefs-fn redef-get-env-empty
    #(is (= t/default-auth-private-file-loc
            (t/key-path-from-env :auth-private-key :private))))
  (with-redefs-fn redef-get-env-empty
    #(is (= t/default-auth-public-file-loc
            (t/key-path-from-env :auth-public-key :public))))
  (with-redefs-fn redef-get-env
    #(is (= (get env-authn "AUTH_PRIVATE_KEY")
            (t/key-path-from-env :auth-private-key :private))))
  (with-redefs-fn redef-get-env
    #(is (= (get env-authn "AUTH_PUBLIC_KEY")
            (t/key-path-from-env :auth-public-key :public)))))

(deftest test-do-read-key
  (is (thrown? java.lang.AssertionError (t/do-read-key "/foo/bar" :broken))))