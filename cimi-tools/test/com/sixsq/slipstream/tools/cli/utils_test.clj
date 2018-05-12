(ns com.sixsq.slipstream.tools.cli.utils-test
  (:require
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.tools.cli.utils :as u])
  (:import
    (java.util.regex Pattern)))


(deftest check-split-credentials
  (are [creds expected] (= (u/split-creds creds) expected)
                        nil [nil nil]
                        true [nil nil]
                        10 [nil nil]
                        "" ["" nil]
                        "user:pass" ["user" "pass"]))


(deftest test-modify-vals
  (let [resource {:ip  "1.2.3.4"
                  :url "http://1.2.3.4/uri"
                  :dns "https://example.com"
                  :multi {:level {:map "https://1.2.3.4/untouched"}}}
        modifiers [[#"1.2.3.4" "4.3.2.1"]
                   [#"example.com" "nuv.la"]]
        m (u/modify-vals resource modifiers)]
    (is (= m {:ip  "4.3.2.1"
              :url "http://4.3.2.1/uri"
              :dns "https://nuv.la"
              :multi {:level {:map "https://1.2.3.4/untouched"}}}))))


(deftest test-->config-resource
  (is (= "https://example.com/api/configuration/slipstream" (u/ss-cfg-url "https://example.com"))))


(deftest test-remove-attrs
  (is (= {} (u/remove-attrs {})))
  (is (= {:foo "bar"} (u/remove-attrs {:foo "bar"})))
  (is (= {:cloudServiceType "foo"} (u/remove-attrs {:cloudServiceType "foo"})))
  (is (nil? (:securityGroup (u/remove-attrs {:cloudServiceType "ec2" :securityGroup "secure"}))))
  (is (nil? (:pdiskEndpoint (u/remove-attrs {:cloudServiceType "nuvlabox" :pdiskEndpoint "endpoint"})))))


(deftest check-resource-type-from-str
  (are [expected arg] (= expected (u/resource-type-from-str arg))
                      nil nil
                      nil ""
                      nil "/uuid"
                      "my-type" "my-type/"
                      "my-type" "my-type/uuid"
                      "my-type" "my-type-template/"
                      "my-type" "my-type-template/uuid"))


(deftest check-resource-type
  (are [expected arg] (= expected (u/resource-type arg))
                      "my-type" "my-type/uuid"
                      "my-type" {:id "my-type/uuid"}
                      "my-type" {:body {:id "my-type/uuid"}}
                      nil {:body {}}))


(deftest test-->re-match-replace
  (let [[pattern replacement] (u/parse-replacement "a=b")]
    (is (= Pattern (type pattern)))
    (is (= "a" (str pattern)))
    (is (= "b" replacement))))
