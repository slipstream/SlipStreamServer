(ns com.sixsq.slipstream.ssclj.resources.spec.service-attribute
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(def prefix-regex #"^[a-z]([a-z0-9-]*[a-z0-9])?$")
(s/def :cimi.service-attribute/prefix (s/and string? #(re-matches prefix-regex %)))

(s/def :cimi.service-attribute/attributeName ::cimi-core/nonblank-string)

(s/def :cimi.service-attribute/type ::cimi-core/nonblank-string)

(s/def :cimi/service-attribute
  (su/only-keys-maps cimi-common/common-attrs
                     {:req-un [::cimi-common/name           ;; name is required
                               ::cimi-common/description    ;; description is required
                               :cimi.service-attribute/prefix
                               :cimi.service-attribute/attributeName
                               :cimi.service-attribute/type]}))
