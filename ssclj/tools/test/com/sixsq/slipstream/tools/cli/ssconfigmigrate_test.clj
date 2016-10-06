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

(deftest test-->re-match-replace
  (let [[m r] (cm/->re-match-replace "a=b")]
    (is (= java.util.regex.Pattern (type m)))
    (is (= "a" (str m)))
    (is (= "b" r))))

(alter-var-root #'cm/*modifiers* (fn [_] #{[#"1.2.3.4" "4.3.2.1"]
                                           [#"example.com" "nuv.la"]}))

(deftest test-modify-vals
  (let [m (cm/modify-vals {:ip  "1.2.3.4"
                           :url "http://1.2.3.4/uri"
                           :dns "https://example.com"
                           })]
    (is (= "4.3.2.1" (:ip m)))
    (is (= "http://4.3.2.1/uri" (:url m)))
    (is (= "https://nuv.la" (:dns m)))
    ))

