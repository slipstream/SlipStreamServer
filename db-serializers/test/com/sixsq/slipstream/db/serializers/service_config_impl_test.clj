(ns com.sixsq.slipstream.db.serializers.service-config-impl-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]))

(defn not-empty-string?
  [x]
  (and (string? x) (not (empty? x))))

(deftest test-get-param-description
  (let [pd (sci/get-sc-param-meta-only "slipstream.base.url")]
    (is (not-empty-string? (.getDescription pd)))))

