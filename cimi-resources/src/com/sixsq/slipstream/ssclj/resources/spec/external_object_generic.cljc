(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-generic
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template :as eot]))


(s/def :cimi.external-object-template.generic/objectStoreCred :cimi.common/resource-link)
(s/def :cimi.external-object-template.generic/bucketName :cimi.core/nonblank-string)
(s/def :cimi.external-object-template.generic/objectName :cimi.core/nonblank-string)

;; Defines the contents of the generic ExternalObjectTemplate resource itself.
(s/def :cimi/external-object-template.generic
    (su/only-keys-maps eot/resource-keys-spec
                       {:req-un [:cimi.external-object-template.generic/objectStoreCred
                                 :cimi.external-object-template.generic/bucketName]
                        :opt-un [:cimi.external-object-template.generic/objectName]}))

;; Defines the contents of the generic template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :externalObjectTemplate here.
(s/def :cimi.external-object-template.generic/externalObjectTemplate
    (su/only-keys-maps eot/template-keys-spec
                       {:req-un [:cimi.external-object-template.generic/objectStoreCred
                                 :cimi.external-object-template.generic/bucketName]
                        :opt-un [:cimi.external-object-template.generic/objectName]}))

(s/def :cimi/external-object-template.generic-create
    (su/only-keys-maps eot/create-keys-spec
                       {:opt-un [:cimi.external-object-template.generic/externalObjectTemplate]}))