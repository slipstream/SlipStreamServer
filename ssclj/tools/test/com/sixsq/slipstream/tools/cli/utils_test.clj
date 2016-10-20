(ns com.sixsq.slipstream.tools.cli.utils-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.tools.cli.utils :as u]))

(deftest test-modify-vals
  (let [m (u/modify-vals {:ip  "1.2.3.4"
                          :url "http://1.2.3.4/uri"
                          :dns "https://example.com"}
                         #{[#"1.2.3.4" "4.3.2.1"]
                           [#"example.com" "nuv.la"]})]
    (is (= "4.3.2.1" (:ip m)))
    (is (= "http://4.3.2.1/uri" (:url m)))
    (is (= "https://nuv.la" (:dns m)))))

(deftest test-->config-resource
  (is (= "/configuration" (u/->config-resource ""))))

(deftest test-remove-attrs
  (is (= {} (u/remove-attrs {})))
  (is (= {:foo "bar"} (u/remove-attrs {:foo "bar"})))
  (is (= {:cloudServiceType "foo"} (u/remove-attrs {:cloudServiceType "foo"})))
  (is (not (contains? (u/remove-attrs {:cloudServiceType "ec2" :securityGroup "secure"}) :securityGroup)))
  (is (not (contains? (u/remove-attrs {:cloudServiceType "nuvlabox" :pdiskEndpoint "endpoint"}) :pdiskEndpoint))))

(deftest test-->re-match-replace
  (let [[m r] (u/->re-match-replace "a=b")]
    (is (= java.util.regex.Pattern (type m)))
    (is (= "a" (str m)))
    (is (= "b" r))))

