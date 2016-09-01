(ns com.sixsq.slipstream.db.serializers.utils-test
  (:require
    [clojure.test :refer :all]
    [schema.core :as sch]
    [com.sixsq.slipstream.db.serializers.test-utils :as tu]
    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as sd]))

(def sc-from-xml-params (.getParameters (tu/conf-xml->sc)))

(def scp-only-value (get sc-from-xml-params "exoscale-ch-gva.endpoint"))
(def scp-with-enum (get sc-from-xml-params "exoscale-ch-gva.orchestrator.instance.type"))
(def scp-with-instructions (get sc-from-xml-params "slipstream.mail.username"))

(deftest test-validate-param-desc
  (is (nil? (sch/check sd/ParameterDescription (u/desc-from-param scp-only-value))))
  (is (nil? (sch/check sd/ParameterDescription (u/desc-from-param scp-with-instructions))))
  (is (nil? (sch/check sd/ParameterDescription (u/desc-from-param scp-with-enum)))))
