(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]))

;; Parameters for the OpenID Connect endpoint, credentials, etc. are picked
;; up from the environment.  Consequently, so parameters are necessary at
;; the moment.  This will have to change to allow multiple OIDC servers to
;; be supported.

(s/def :cimi.session-template.oidc/redirectURI :cimi.core/nonblank-string)

;; all parameters must be specified in both the template and the create resource
(def session-template-keys-spec-opt
  {:opt-un [:cimi.session-template.oidc/redirectURI]})

;; Defines the contents of the oidc SessionTemplate resource itself.
(s/def :cimi/session-template.oidc
  (su/only-keys-maps ps/resource-keys-spec
                     session-template-keys-spec-opt))

;; Defines the contents of the oidc template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def :cimi.session-template.oidc/sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     session-template-keys-spec-opt))

(s/def :cimi/session-template.oidc-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.session-template.oidc/sessionTemplate]}))
