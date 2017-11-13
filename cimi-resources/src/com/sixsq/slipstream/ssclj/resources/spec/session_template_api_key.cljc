(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-api-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]))

(s/def :cimi.session-template.api-key/key :cimi.core/nonblank-string)
(s/def :cimi.session-template.api-key/secret :cimi.core/nonblank-string)

;; all parameters must be specified in both the template and the create resource
(def session-template-keys-spec
  {:req-un [:cimi.session-template.api-key/key
            :cimi.session-template.api-key/secret]})

;; Defines the contents of the api-key SessionTemplate resource itself.
(s/def :cimi/session-template.api-key
  (su/only-keys-maps ps/resource-keys-spec
                     session-template-keys-spec))

;; Defines the contents of the api-key template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def :cimi.session-template.api-key/sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     session-template-keys-spec))

(s/def :cimi/session-template.api-key-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [:cimi.session-template.api-key/sessionTemplate]}))
