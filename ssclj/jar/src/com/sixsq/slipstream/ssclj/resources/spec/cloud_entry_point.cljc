(ns com.sixsq.slipstream.ssclj.resources.spec.cloud-entry-point
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def :cimi.cloud-entry-point/baseURI :cimi.core/nonblank-string)

(s/def :cimi/cloud-entry-point
  (su/constrained-map keyword? :cimi.common/resource-link
                      c/common-attrs
                      {:req-un [:cimi.cloud-entry-point/baseURI]}))
