(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-oidc
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]))

(s/def :cimi.configuration-template.session/methodKey :cimi.core/token)
(s/def :cimi.configuration-template.session/clientID :cimi.core/token)
(s/def :cimi.configuration-template.session/baseURL :cimi.core/token)
(s/def :cimi.configuration-template.session/publicKey :cimi.core/token)

(def configuration-template-keys-spec-req
  {:req-un [:cimi.configuration-template.session/methodKey
            :cimi.configuration-template.session/clientID
            :cimi.configuration-template.session/baseURL
            :cimi.configuration-template.session/publicKey]})

;; Defines the contents of the slipstream ConfigurationTemplate resource itself.
(s/def :cimi/configuration-template.session
  (su/only-keys-maps ps/resource-keys-spec
                     configuration-template-keys-spec-req))

;; Defines the contents of the slipstream template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :configurationTemplate here.
(s/def :cimi.configuration-template.session/configurationTemplate
  (su/only-keys-maps ps/template-keys-spec
                     configuration-template-keys-spec-req))

(s/def :cimi/configuration-template.session-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.configuration-template.slipstream/configurationTemplate]}))
