(ns com.sixsq.slipstream.ssclj.usage.daily-summary-launcher-test
  (:require 
    [com.sixsq.slipstream.ssclj.usage.daily-summary-launcher 	:refer :all]    
    [clojure.test                               				:refer :all]))

(deftest main-can-be-launched-without-args
  (-main))

(deftest main-can-be-launched-with-args
  (-main "-d" "2015-01-01"))