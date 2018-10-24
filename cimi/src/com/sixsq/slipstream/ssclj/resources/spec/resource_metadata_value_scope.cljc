(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope
  "schema definitions for the 'vscope' field of a ResourceMetadata resource"
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-unit :as unit]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-single-value :as single-value]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-range :as range]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-enumeration :as enumeration]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-item :as item]
    [spec-tools.core :as st]))


(s/def ::value-scope (s/or :unit ::unit/unit
                           :single-value ::single-value/single-value
                           :range ::range/range
                           :enumeration ::enumeration/enumeration
                           :item ::item/collection-item))

;; FIXME: This function shouldn't be necessary!
;; There is a problem when using the ::value-scope spec directly in the
;; s/map-of expression in st/spec.  Validation throws an exception when
;; trying to validate against single-value or collection-item.  Hiding
;; the details behind this function works, but clearly isn't ideal for
;; error reporting. The reason for the problem needs to be determined
;; and either worked around or fixed.
(defn valid-value?
  [x]
  (s/valid? ::value-scope x))


(s/def ::vscope
  (st/spec {:spec                  (s/map-of keyword? valid-value? :min-count 1)
            :slipstream.es/mapping {:type "object", :enabled false}}))
