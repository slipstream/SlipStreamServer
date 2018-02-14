(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-report
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template :as eot]))


(s/def :cimi.external-object-template.report/runUUID :cimi.core/nonblank-string)
(s/def :cimi.external-object-template.report/component :cimi.core/nonblank-string)

;; Defines the contents of the report ExternalObjectTemplate resource itself.
(s/def :cimi/external-object-template.report
    (su/only-keys-maps eot/resource-keys-spec
                       {:req-un [:cimi.external-object-template.report/runUUID
                                 :cimi.external-object-template.report/component]}))

;; Defines the contents of the report template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :externalObjectTemplate here.
(s/def :cimi.external-object-template.report/externalObjectTemplate
    (su/only-keys-maps eot/template-keys-spec
                       {:req-un [:cimi.external-object-template.report/runUUID
                                 :cimi.external-object-template.report/component]}))

(s/def :cimi/external-object-template.report-create
    (su/only-keys-maps eot/create-keys-spec
                       {:opt-un [:cimi.external-object-template.report/externalObjectTemplate]}))