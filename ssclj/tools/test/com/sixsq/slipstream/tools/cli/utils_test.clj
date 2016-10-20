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
