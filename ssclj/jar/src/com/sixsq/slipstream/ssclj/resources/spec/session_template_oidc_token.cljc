(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-oidc-token
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]))

;; The OIDC token must be provided as part of the template.
(s/def :cimi.session-template.oidc-token/token :cimi.core/nonblank-string)

(def oidc-token-keys {:req-un [:cimi.session-template.oidc-token/token]})

;; Defines the contents of the oidc token SessionTemplate resource itself.
(s/def :cimi/session-template.oidc-token
  (su/only-keys-maps ps/resource-keys-spec
                     oidc-token-keys))

;; Defines the contents of the oidc token template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def :cimi.session-template.oidc-token/sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     oidc-token-keys))

(s/def :cimi/session-template.oidc-token-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.session-template.oidc-token/sessionTemplate]}))
