(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-template-std
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template :as ps]))

(s/def :cimi.deployment-template.std/module :cimi.core/nonblank-string)

(def deployment-template-keys-spec-req
  {:req-un [:cimi.deployment-template.std/module]})

(def deployment-template-create-keys-spec-req
  {:req-un [:cimi.deployment-template.std/module]})

;; Defines the contents of the std deploymentTemplate resource itself.
(s/def :cimi/deployment-template.std
  (su/only-keys-maps ps/resource-keys-spec
                     deployment-template-keys-spec-req))

;; Defines the contents of the std template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :deploymentTemplate here.
(s/def :cimi.deployment-template.std/deploymentTemplate
  (su/only-keys-maps ps/template-keys-spec
                     deployment-template-create-keys-spec-req))

(s/def :cimi/deployment-template.std-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.deployment-template.std/deploymentTemplate]}))
