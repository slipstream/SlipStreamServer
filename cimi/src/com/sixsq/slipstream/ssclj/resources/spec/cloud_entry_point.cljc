(ns com.sixsq.slipstream.ssclj.resources.spec.cloud-entry-point
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::baseURI ::cimi-core/nonblank-string)


(s/def ::cloud-entry-point
  (su/constrained-map keyword? ::cimi-common/resource-link
                      cimi-common/common-attrs
                      {:req-un [::baseURI]}))
