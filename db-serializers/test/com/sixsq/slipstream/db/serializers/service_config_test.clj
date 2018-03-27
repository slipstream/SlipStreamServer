(ns com.sixsq.slipstream.db.serializers.service-config-test
  (:require
    [clojure.test :refer :all]

    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [com.sixsq.slipstream.ssclj.resources.spec.description]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-slipstream :as crtpls]))

;; Tests

(deftest test-attrs-map-valid
  (let [config-attrs (set (map (comp keyword name) (:req-un crtpls/configuration-template-keys-spec-req)))]
    (is (= (set (keys sci/rname->param))
           config-attrs))))

