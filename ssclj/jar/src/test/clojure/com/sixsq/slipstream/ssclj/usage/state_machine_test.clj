(ns com.sixsq.slipstream.ssclj.usage.state-machine-test
  (:require 
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.usage.state-machine :refer :all]))

(defn leads-to   
  [state trigger expected-action]
  (is (= expected-action (action state trigger))))

(deftest transitions
  (leads-to :initial :start :insert-start)
  (leads-to :initial :stop  :severe-wrong-transition)

  (leads-to :started :start :severe-wrong-transition)
  (leads-to :started :stop  :close-record)

  (leads-to :stopped :start :reset-end)
  (leads-to :stopped :stop  :severe-wrong-transition))