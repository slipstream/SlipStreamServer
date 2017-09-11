(ns com.sixsq.slipstream.ssclj.resources.deployment.state-machine-test
  (:require [clojure.test :refer [deftest are is]]
            [com.sixsq.slipstream.ssclj.resources.deployment.state-machine :refer :all]))

(deftest next-state
  (is (= "executing" (get-next-state "provisioning"))))
