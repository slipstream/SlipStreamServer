(ns com.sixsq.slipstream.auth.utils.certs-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [buddy.core.keys :as ks]
    [environ.core :as environ]
    [com.sixsq.slipstream.auth.env-fixture :as env-fixture]
    [com.sixsq.slipstream.auth.utils.certs :as t]))

(def test-rsa-key "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAosD2Dkf0aa44Q5ur6RNOhVlUbF/kWzQq4UR6nm4cjX1BbnJ+gJdlPYMvg7iu+YCDHPZXERDMXLc4jk3Is9IVtSB2DLbrRYSQGRkHbdo7RF9RZclL1kXlxQUOyU9bvxtbc6oFNuL9WpohEOuPedLvbb5gSNrJaH9gnKkESoMmfViA8O2U4MXcuZ3bbS8spL5LCIPXYDPcpDBjFFvQgFKNvCChb+i6KuU07923T6O0HBkJVmuJ7pRPW6atYADIJ3xYkC5CGE5xqc6KOUibl07DhWP4C8cjN00DdyDazogsKqTXWlFzMOknwlz0fWOtDCvDdvD8AwOsrpU2QAzuLmXDWQIDAQAB")

(deftest test-key-path
  (with-redefs [environ/env {}]
    (is (= t/default-private-key-path
           (t/key-path :auth-private-key t/default-private-key-path)))
    (is (= t/default-public-key-path
           (t/key-path :auth-public-key t/default-public-key-path))))

  (with-redefs [environ/env env-fixture/env-map]
    (is (= (get env-fixture/env-authn "AUTH_PRIVATE_KEY")
           (t/key-path :auth-private-key t/default-private-key-path)))
    (is (= (get env-fixture/env-authn "AUTH_PUBLIC_KEY")
           (t/key-path :auth-public-key t/default-public-key-path)))))

(deftest check-read-key
  (with-redefs [environ/env env-fixture/env-map]
    (is (t/read-key ks/private-key t/default-private-key-path :auth-private-key))
    (is (t/private-key :auth-private-key))
    (is (t/read-key ks/public-key t/default-public-key-path :auth-public-key))
    (is (t/public-key :auth-public-key))))

(deftest check-throws-unknown-key
  (with-redefs [t/key-path (fn [_ _ ] "/unknown/key-path.pem")
                environ.core/env {}]
    (is (thrown? Exception (t/read-key ks/private-key t/default-private-key-path :auth-private-key)))
    (is (thrown? Exception (t/read-key ks/public-key t/default-public-key-path :auth-public-key)))))

(deftest check-parse-key-string
  (is (t/parse-key-string test-rsa-key))
  (is (thrown? Exception (t/parse-key-string (str test-rsa-key "-invalid")))))
