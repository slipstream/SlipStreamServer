(ns com.sixsq.slipstream.ssclj.resources.spec.external-object
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]))

(s/def ::objectType ::cimi-core/identifier)
(s/def ::state #{"new" "ready"})
(s/def ::objectStoreCred ::cimi-common/resource-link)
(s/def ::objectName ::cimi-core/nonblank-string)
(s/def ::bucketName ::cimi-core/nonblank-string)

(s/def ::contentType ::cimi-core/nonblank-string)

(def external-object-template-regex #"^external-object-template/[a-z]+(-[a-z]+)*$")
(s/def ::href (s/and string? #(re-matches external-object-template-regex %)))

(def external-object-keys-spec {:req-un [::objectType
                                         ::state
                                         ::objectName
                                         ::bucketName
                                         ::objectStoreCred]
                                :opt-un [::contentType
                                         ::href]})
