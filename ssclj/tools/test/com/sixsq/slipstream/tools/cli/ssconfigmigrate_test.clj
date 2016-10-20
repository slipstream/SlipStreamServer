(ns com.sixsq.slipstream.tools.cli.ssconfigmigrate-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.tools.cli.ssconfigmigrate :as cm]
    [com.sixsq.slipstream.tools.cli.utils :as u]))

(deftest test-remove-attrs
  (is (= {} (cm/remove-attrs {})))
  (is (= {:foo "bar"} (cm/remove-attrs {:foo "bar"})))
  (is (= {:cloudServiceType "foo"} (cm/remove-attrs {:cloudServiceType "foo"})))
  (is (not (contains? (cm/remove-attrs {:cloudServiceType "ec2" :securityGroup "secure"}) :securityGroup)))
  (is (not (contains? (cm/remove-attrs {:cloudServiceType "nuvlabox" :pdiskEndpoint "endpoint"}) :pdiskEndpoint))))

(deftest test-->re-match-replace
  (let [[m r] (u/->re-match-replace "a=b")]
    (is (= java.util.regex.Pattern (type m)))
    (is (= "a" (str m)))
    (is (= "b" r))))

(alter-var-root #'cm/*modifiers* (fn [_] #{[#"1.2.3.4" "4.3.2.1"]
                                           [#"example.com" "nuv.la"]}))

