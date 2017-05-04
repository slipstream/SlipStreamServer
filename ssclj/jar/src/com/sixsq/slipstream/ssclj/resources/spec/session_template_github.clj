(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-github
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]))

;; Parameters for the OpenID Connect endpoint, credentials, etc. are picked
;; up from the environment.  Consequently, so parameters are necessary at
;; the moment.  This will have to change to allow multiple OIDC servers to
;; be supported.

;; Defines the contents of the github SessionTemplate resource itself.
(s/def :cimi/session-template.github
  (su/only-keys-maps ps/resource-keys-spec))

;; Defines the contents of the github template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def :cimi.session-template.github/sessionTemplate
  (su/only-keys-maps ps/template-keys-spec))

(s/def :cimi/session-template.github-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.session-template.github/sessionTemplate]}))
