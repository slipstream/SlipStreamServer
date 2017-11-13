(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-github
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]))

(s/def :cimi.configuration-template.session-github/clientID :cimi.core/token)
(s/def :cimi.configuration-template.session-github/clientSecret :cimi.core/token)

(def configuration-template-keys-spec-req
  {:req-un [:cimi.configuration-template/instance
            :cimi.configuration-template.session-github/clientID
            :cimi.configuration-template.session-github/clientSecret]})

(def configuration-template-keys-spec-create
  {:req-un [:cimi.configuration-template/instance
            :cimi.configuration-template.session-github/clientID
            :cimi.configuration-template.session-github/clientSecret]})

;; Defines the contents of the github authentication ConfigurationTemplate resource itself.
(s/def :cimi/configuration-template.session-github
  (su/only-keys-maps ps/resource-keys-spec
                     configuration-template-keys-spec-req))

;; Defines the contents of the github authentication template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :configurationTemplate here.
(s/def :cimi.configuration-template.session-github/configurationTemplate
  (su/only-keys-maps ps/template-keys-spec
                     configuration-template-keys-spec-create))

(s/def :cimi/configuration-template.session-github-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.configuration-template.session-github/configurationTemplate]}))
