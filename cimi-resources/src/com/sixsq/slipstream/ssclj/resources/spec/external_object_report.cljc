(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-report
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template :as eot]))


;; Defines the contents of the report ExternalObjectTemplate resource itself.
(s/def :cimi/external-object-template.report
    (su/only-keys-maps eot/resource-keys-spec))

;; Defines the contents of the report template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :externalObjectTemplate here.
(s/def :cimi.external-object-template.report/externalObjectTemplate
    (su/only-keys-maps eot/template-keys-spec))

(s/def :cimi/external-object-template.report-create
    (su/only-keys-maps eot/create-keys-spec
                       {:opt-un [:cimi.external-object-template.report/externalObjectTemplate]}))