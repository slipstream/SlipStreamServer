(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-test
  (:require
    [clojure.test :refer [are deftest is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope :as spec]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-item-test :as item]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-enumeration-test :as enumeration]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-range-test :as range]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-single-value-test :as single-value]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-unit-test :as unit]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid {:alpha enumeration/valid
            :beta  range/valid
            :gamma single-value/valid
            :delta unit/valid
            :zeta  item/valid})


(deftest check-value-scope

  (s/explain ::spec/vscope valid)

  (stu/is-valid ::spec/vscope valid)

  (stu/is-invalid ::spec/vscope {:badAttribute 1}))
