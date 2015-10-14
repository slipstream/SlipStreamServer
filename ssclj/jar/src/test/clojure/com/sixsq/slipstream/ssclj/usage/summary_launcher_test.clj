(ns com.sixsq.slipstream.ssclj.usage.summary-launcher-test
  (:require 
    [com.sixsq.slipstream.ssclj.usage.daily-summary-launcher 	  :as dl]
    [com.sixsq.slipstream.ssclj.usage.monthly-summary-launcher  :as ml]
    [clojure.test                               				        :refer :all]))

(deftest daily-main-can-be-launched-without-args
  (dl/-main))

(deftest daily-main-can-be-launched-with-args
  (dl/-main "-d" "2015-01-01"))

(deftest daily-main-checks-args
  (is (thrown? IllegalArgumentException (dl/-main "-x" "2015-01-01")))
  (is (thrown? IllegalArgumentException (dl/-main "-d" "2015-01"))))

(deftest monthly-main-can-be-launched-without-args
  (ml/-main))

(deftest monthly-main-can-be-launched-with-args
  (ml/-main "-m" "2015-01")
  (ml/-main "-m" "2015-01" "-e" "joe,jack"))

(deftest monthly-main-checks-args
  (is (thrown? IllegalArgumentException (ml/-main "-x" "2015-01")))
  (is (thrown? IllegalArgumentException (ml/-main "-m" "2015-01-01"))))
