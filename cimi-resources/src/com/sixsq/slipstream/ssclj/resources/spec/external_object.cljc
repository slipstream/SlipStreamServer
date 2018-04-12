(ns com.sixsq.slipstream.ssclj.resources.spec.external-object
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common]))

(s/def :cimi.external-object/objectType :cimi.core/identifier)
(s/def :cimi.external-object/state #{"new" "ready"})
(s/def :cimi.external-object/objectStoreCred :cimi.common/resource-link)
(s/def :cimi.external-object/objectName :cimi.core/nonblank-string)
(s/def :cimi.external-object/bucketName :cimi.core/nonblank-string)

(s/def :cimi.external-object/contentType :cimi.core/nonblank-string)

(def external-object-template-regex #"^external-object-template/[a-z]+(-[a-z]+)*$")
(s/def :cimi.external-object/href (s/and string? #(re-matches external-object-template-regex %)))

(def external-object-keys-spec {:req-un [:cimi.external-object/objectType
                                         :cimi.external-object/state
                                         :cimi.external-object/objectName
                                         :cimi.external-object/bucketName
                                         :cimi.external-object/objectStoreCred]
                                :opt-un [:cimi.external-object/contentType
                                         :cimi.external-object/href]})
