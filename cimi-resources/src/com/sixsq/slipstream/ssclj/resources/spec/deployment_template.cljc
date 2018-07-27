(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::module ::cimi-common/resource-link)


(def deployment-template-keys-spec {:req-un [::module]})

(s/def ::deployment-template (su/only-keys-maps
                               cimi-common/common-attrs
                               deployment-template-keys-spec))

;; Defines the contents of the generic template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :deploymentTemplate here.
(s/def ::deploymentTemplate
  (su/only-keys-maps cimi-common/template-attrs
                     deployment-template-keys-spec))

(s/def ::deployment-template-create
  (su/only-keys-maps cimi-common/create-attrs
                     {:req-un [::deploymentTemplate]}))
