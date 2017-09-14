(ns com.sixsq.slipstream.ssclj.resources.deployment.state-machine-test
  (:require [clojure.test :refer [deftest are is]]
            [com.sixsq.slipstream.ssclj.resources.deployment.state-machine :refer :all]))

(deftest next-state
  (is (= executing-state (get-next-state provisioning-state))))

(deftest competed-run
  (is (is-completed? done-state))
  (is (is-completed? cancelled-state))
  (is (not (is-completed? executing-state))))

(deftest can-terminate
  (is (can-terminate? ready-state))
  (is (can-terminate? aborted-state))
  (is (can-terminate? cancelled-state))
  (is (not (can-terminate? executing-state))))