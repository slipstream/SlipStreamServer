(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-cyclone
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]))

;; Parameters for the OpenID Connect endpoint, credentials, etc. are picked
;; up from the environment.

;; Defines the contents of the cyclone SessionTemplate resource itself.
(s/def :cimi/session-template.cyclone
  (su/only-keys-maps ps/resource-keys-spec))

;; Defines the contents of the cyclone template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def :cimi.session-template.cyclone/sessionTemplate
  (su/only-keys-maps ps/template-keys-spec))

(s/def :cimi/session-template.cyclone-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.session-template.cyclone/sessionTemplate]}))
