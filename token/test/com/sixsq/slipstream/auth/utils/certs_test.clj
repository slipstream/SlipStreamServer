(ns com.sixsq.slipstream.auth.utils.certs-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [buddy.sign.util :as util]
    [environ.core :as environ]
    [com.sixsq.slipstream.auth.env-fixture :as env-fixture]
    [com.sixsq.slipstream.auth.utils.certs :as t]))

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
    (is (t/read-private-key :auth-private-key))
    (is (t/private-key :auth-private-key))
    (is (t/read-public-key :auth-public-key))
    (is (t/public-key :auth-public-key))))

(deftest check-throws-unknown-key
  (with-redefs [t/key-path (fn [_ _ ] "/unknown/key-path.pem")
                environ.core/env {}]
    (is (thrown? Exception (t/read-private-key :auth-private-key)))
    (is (thrown? Exception (t/read-public-key :auth-public-key)))))
