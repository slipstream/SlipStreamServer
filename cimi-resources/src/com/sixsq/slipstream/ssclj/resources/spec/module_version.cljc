(ns com.sixsq.slipstream.ssclj.resources.spec.module-version
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.module :as mspec]))


;; FIXME: Should the module version repeat the common module attributes?
(s/def :cimi/module-version
  (su/constrained-map keyword? any?
                      c/common-attrs
                      mspec/common-module-attrs))
