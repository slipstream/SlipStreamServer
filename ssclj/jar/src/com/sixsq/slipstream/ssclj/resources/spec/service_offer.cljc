(ns com.sixsq.slipstream.ssclj.resources.spec.service-offer
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; FIXME: This should use the standard href definition instead.
(s/def :cimi.service-offer/href :cimi.core/nonblank-string)
(s/def :cimi.service-offer/connector (su/only-keys :req-un [:cimi.service-offer/href]))

(s/def :cimi/service-offer
  (su/constrained-map keyword? any?
                      c/common-attrs
                      {:req-un [:cimi.service-offer/connector]}))
