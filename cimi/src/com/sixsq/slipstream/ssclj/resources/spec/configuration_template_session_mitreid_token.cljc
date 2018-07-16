(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-mitreid-token
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::publicKey ::cimi-core/nonblank-string)             ;; allows jwk JSON representation
(s/def ::authzClientIP ::cimi-core/token)
(s/def ::authnMethod ::cimi-core/token)


(def configuration-template-keys-spec-req
  {:req-un [::ps/instance ::publicKey ::authzClientIP ::authnMethod]})


(def configuration-template-keys-spec-create
  {:req-un [::ps/instance ::publicKey ::authzClientIP ::authnMethod]})


;; Defines the contents of the OIDC authentication ConfigurationTemplate resource itself.
(s/def ::session-mitreid-token
  (su/only-keys-maps ps/resource-keys-spec
                     configuration-template-keys-spec-req))


;; Defines the contents of the OIDC authentication template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :configurationTemplate here.
(s/def ::configurationTemplate
  (su/only-keys-maps ps/template-keys-spec
                     configuration-template-keys-spec-create))


(s/def ::session-mitreid-token-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::configurationTemplate]}))
