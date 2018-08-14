(ns com.sixsq.slipstream.ssclj.resources.spec.storage-bucket
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::credentials (s/coll-of ::cimi-common/resource-link))
(s/def ::bucketName ::cimi-core/nonblank-string)
(s/def ::usage pos-int?)
(s/def ::unit #{"B","KB","MB", "GB"})
(s/def ::connector ::cimi-common/resource-link)
(s/def ::serviceOffer ::cimi-common/resource-link)
(s/def ::externalObject ::cimi-common/resource-link)

(def storage-bucket-specs {:req-un [::credentials
                                    ::bucketName
                                    ::connector
                                    ::usage]
                           :opt-un [::externalObject
                                    ::serviceOffer
                                    ::unit]})

(def storage-bucket-keys-spec (su/merge-keys-specs [cimi-common/common-attrs
                                                    storage-bucket-specs]))

(s/def ::storage-bucket (su/only-keys-maps storage-bucket-keys-spec))
