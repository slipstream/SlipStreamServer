(ns com.sixsq.slipstream.tools.cli.ssconfigmigrate-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.tools.cli.ssconfigmigrate :as cm]))

(deftest test-remove-attrs
  (is (= {} (cm/remove-attrs {})))
  (is (= {:foo "bar"} (cm/remove-attrs {:foo "bar"})))
  (is (= {:cloudServiceType "foo"} (cm/remove-attrs {:cloudServiceType "foo"})))
  (is (not (contains? (cm/remove-attrs {:cloudServiceType "ec2" :securityGroup "secure"}) :securityGroup)))
  (is (not (contains? (cm/remove-attrs {:cloudServiceType "nuvlabox" :pdiskEndpoint "endpoint"}) :pdiskEndpoint))))

