(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-mitreid-token
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::token ::cimi-core/nonblank-string)

;; all parameters must be specified in both the template and the create resource
(def session-template-keys-spec
  {:req-un [::token]})

;; Defines the contents of the oidc-token SessionTemplate resource itself.
(s/def ::mitreid-token
  (su/only-keys-maps ps/resource-keys-spec
                     session-template-keys-spec))

;; Defines the contents of the oidc-token template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def ::sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     session-template-keys-spec))

(s/def ::mitreid-token-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::sessionTemplate]}))
