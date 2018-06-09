(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-internal
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::username ::cimi-core/nonblank-string)
(s/def ::password ::cimi-core/nonblank-string)

;; all parameters must be specified in both the template and the create resource
(def session-template-keys-spec-req
  {:req-un [::username ::password]})

;; Defines the contents of the internal SessionTemplate resource itself.
(s/def :cimi/session-template.internal
  (su/only-keys-maps ps/resource-keys-spec
                     session-template-keys-spec-req))

;; Defines the contents of the internal template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def ::sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     session-template-keys-spec-req))

(s/def :cimi/session-template.internal-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::sessionTemplate]}))
