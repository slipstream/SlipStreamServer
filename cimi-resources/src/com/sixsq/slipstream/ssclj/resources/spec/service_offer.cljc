(ns com.sixsq.slipstream.ssclj.resources.spec.service-offer
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.connector-template]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;;
;; Must reference the raw name of the connector.  Reference the schema
;; for defining connector names.
;;
(s/def ::href :cimi.connector-template.core/identifier)
(s/def ::connector (su/only-keys :req-un [::href]))

(s/def ::service-offer
  (su/constrained-map keyword? any?
                      c/common-attrs
                      {:req-un [::connector]}))
