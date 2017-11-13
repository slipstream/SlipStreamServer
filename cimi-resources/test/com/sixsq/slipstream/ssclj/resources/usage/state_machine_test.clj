(ns com.sixsq.slipstream.ssclj.resources.usage.state-machine-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.usage.state-machine :refer :all]))

(defn leads-to
  [state trigger expected-action]
  (is (= expected-action (action state trigger))))

(deftest transitions
  (leads-to :initial :start :insert-start)
  (leads-to :initial :stop :wrong-transition)

  (leads-to :started :start :do-nothing)
  (leads-to :started :stop :close-record)

  (leads-to :stopped :start :insert-start)
  (leads-to :stopped :stop :wrong-transition))
