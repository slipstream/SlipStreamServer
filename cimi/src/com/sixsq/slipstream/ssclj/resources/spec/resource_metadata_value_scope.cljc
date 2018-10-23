(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope
  "schema definitions for the 'vscope' field of a ResourceMetadata resource"
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-unit :as unit]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-single-value :as single-value]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-range :as range]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-enumeration :as enumeration]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-item :as item]))


(s/def ::value-scope (s/or :unit ::unit/unit
                           :single-value ::single-value/single-value
                           :range ::range/range
                           :enumeration ::enumeration/enumeration
                           :item ::item/collection-item))


(s/def ::vscope (s/map-of keyword? ::value-scope :min-count 1))
