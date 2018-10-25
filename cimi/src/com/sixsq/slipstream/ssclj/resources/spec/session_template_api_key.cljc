(ns com.sixsq.slipstream.ssclj.resources.spec.session-template-api-key
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template :as ps]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


(s/def ::key
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :json-schema/displayName "Key"
             :json-schema/category "general"
             :json-schema/description "API key"
             :json-schema/type "string"
             :json-schema/mandatory true
             :json-schema/readOnly false
             :json-schema/order 20)))


(s/def ::secret
  (-> (st/spec ::cimi-core/nonblank-string)
      (assoc :json-schema/displayName "Secret"
             :json-schema/category "general"
             :json-schema/description "secret associated with API key"
             :json-schema/type "password"
             :json-schema/mandatory true
             :json-schema/readOnly false
             :json-schema/order 21)))

;; all parameters must be specified in both the template and the create resource
(def session-template-keys-spec
  {:req-un [::key ::secret]})

;; Defines the contents of the api-key SessionTemplate resource itself.
(s/def ::api-key
  (su/only-keys-maps ps/resource-keys-spec
                     session-template-keys-spec))

;; Defines the contents of the api-key template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :sessionTemplate here.
(s/def ::sessionTemplate
  (su/only-keys-maps ps/template-keys-spec
                     session-template-keys-spec))

(s/def ::api-key-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::sessionTemplate]}))
