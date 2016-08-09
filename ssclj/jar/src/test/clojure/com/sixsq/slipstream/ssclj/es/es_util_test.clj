(ns com.sixsq.slipstream.ssclj.es.es-util-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.es.es-util :as esu]))

(deftest test-edn->json
  (is (= "{\"a\":1}"                              (esu/edn->json {:a 1})))
  (is (= "{\"schema-org\\/attr-name\":1}"         (esu/edn->json {:schema-org/attr-name 1})))
  (is (= "{\"schema-org\\/a\\/b\\/attr-name\":1}" (esu/edn->json {:schema-org/a/b/attr-name 1}))))

