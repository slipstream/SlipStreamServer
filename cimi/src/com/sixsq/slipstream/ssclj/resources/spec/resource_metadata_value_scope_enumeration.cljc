(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-value-scope-enumeration
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::values (s/coll-of string? :min-count 1 :kind vector?))


(s/def ::default string?)


(s/def ::enumeration (su/only-keys :req-un [::values]
                                   :opt-un [::default]))
