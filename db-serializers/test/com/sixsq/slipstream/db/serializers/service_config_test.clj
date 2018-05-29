(ns com.sixsq.slipstream.db.serializers.service-config-test
  (:require
    [clojure.test :refer :all]

    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-slipstream :as crtpls]
    [com.sixsq.slipstream.ssclj.resources.spec.description]))


(deftest test-attrs-map-valid
  (is (= (set (keys sci/rname->param))
         (->> (vals crtpls/configuration-template-keys-spec-req)
              (apply concat)
              (map (comp keyword name))
              set))))
