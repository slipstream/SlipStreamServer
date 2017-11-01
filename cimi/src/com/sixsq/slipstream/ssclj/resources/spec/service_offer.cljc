(ns com.sixsq.slipstream.ssclj.resources.spec.service-offer
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.connector-template]))

;;
;; Must reference the raw name of the connector.  Reference the schema
;; for defining connector names.
;;
(s/def :cimi.service-offer/href :cimi.connector-template.core/identifier)
(s/def :cimi.service-offer/connector (su/only-keys :req-un [:cimi.service-offer/href]))

(s/def :cimi/service-offer
  (su/constrained-map keyword? any?
                      c/common-attrs
                      {:req-un [:cimi.service-offer/connector]}))
