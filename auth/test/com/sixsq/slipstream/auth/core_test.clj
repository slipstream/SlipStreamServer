(ns com.sixsq.slipstream.auth.core-test
  (:require [clojure.test                   :refer :all]
            [com.sixsq.slipstream.auth.core :refer :all]
            [buddy.hashers                  :as hashers]))

(deftest test-encrypt-password
  (let [secret "ze secret shh"
        encrypted (hashers/encrypt secret {:algorithm :bcrypt+sha512})]
    (is (hashers/check      secret   encrypted))
    (is (not (hashers/check "wrong" encrypted)))

    (is (= 162 (count encrypted)))
    ))

