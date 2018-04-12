(ns com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report :as eor]))


(def template-resource-keys-spec
  (u/remove-req eor/external-object-report-keys-spec #{:cimi.external-object/state
                                                       :cimi.external-object/objectName
                                                       :cimi.external-object/bucketName
                                                       :cimi.external-object/objectStoreCred}))

(s/def :cimi.external-object-template.report/filename :cimi.core/nonblank-string)

;; Defines the contents of the generic template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :externalObjectTemplate here.
(s/def :cimi.external-object-template.report/externalObjectTemplate
  (su/only-keys-maps c/template-attrs
                     template-resource-keys-spec
                     {:req-un [:cimi.external-object-template.report/filename]}))

(s/def :cimi/external-object-template.report-create
  (su/only-keys-maps c/create-attrs
                     {:req-un [:cimi.external-object-template.report/externalObjectTemplate]}))

