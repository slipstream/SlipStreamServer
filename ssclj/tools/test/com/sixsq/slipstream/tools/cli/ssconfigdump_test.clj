(ns com.sixsq.slipstream.tools.cli.ssconfigdump-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.tools.cli.ssconfigdump :as cd]))

(deftest test-->config-resource
  (is (= "/configuration" (cd/->config-resource ""))))

