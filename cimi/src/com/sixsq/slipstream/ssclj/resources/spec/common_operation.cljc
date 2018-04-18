(ns com.sixsq.slipstream.ssclj.resources.spec.common-operation
  "Spec definitions for common operation types used in CIMI resources."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]))

;; resource operations
(s/def ::href ::cimi-core/nonblank-string)
(s/def ::rel ::cimi-core/nonblank-string)
