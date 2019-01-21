(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-session-mitreid-token
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::clientIPs (s/coll-of ::cimi-core/token :min-count 1 :kind vector?))


(def configuration-template-keys-spec-req
  {:req-un [::ps/instance]
   :opt-un [::clientIPs]})


(def configuration-template-keys-spec-create
  {:req-un [::ps/instance]
   :opt-un [::clientIPs]})


;; Defines the contents of the OIDC authentication ConfigurationTemplate resource itself.
(s/def ::schema
  (su/only-keys-maps ps/resource-keys-spec
                     configuration-template-keys-spec-req))


;; Defines the contents of the OIDC authentication template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :configurationTemplate here.
(s/def ::configurationTemplate
  (su/only-keys-maps ps/template-keys-spec
                     configuration-template-keys-spec-create))


(s/def ::schema-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::configurationTemplate]}))
