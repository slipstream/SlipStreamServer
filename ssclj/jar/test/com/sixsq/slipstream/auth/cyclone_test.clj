(ns com.sixsq.slipstream.auth.cyclone-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.cyclone :as cy]))

(deftest test-login-name
  (is (nil? (cy/login-name {})))
  (is (nil? (cy/login-name {:name nil :preferred_username nil})))
  (is (nil? (cy/login-name {:name "" :preferred_username ""})))

  (is (= "Ringo_Star" (cy/login-name {:name "Ringo Star"})))
  (is (= "Ringo_Star" (cy/login-name {:name "Ringo Star" :preferred_username "Paul McCartney"})))

  (is (= "Paul_McCartney" (cy/login-name {:preferred_username "Paul McCartney"})))
  (is (= "Paul_McCartney" (cy/login-name {:name nil :preferred_username "Paul McCartney"})))
  (is (= "Paul_McCartney" (cy/login-name {:name "" :preferred_username "Paul McCartney"}))))


