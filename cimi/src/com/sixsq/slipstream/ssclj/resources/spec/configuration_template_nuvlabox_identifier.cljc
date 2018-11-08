(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-nuvlabox-identifier
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::name ::cimi-core/nonblank-string)
(s/def ::identifiers (s/coll-of (s/keys :req-un [::name])))

(def configuration-template-keys-spec-req
  {:req-un [::ps/instance ::identifiers]})

(def configuration-template-keys-spec-create
  {:req-un [::ps/instance ::identifiers]})

;; Defines the contents of the nuvlabox-identifier ConfigurationTemplate resource itself.
(s/def ::nuvlabox-identifier
  (su/only-keys-maps ps/resource-keys-spec
                     configuration-template-keys-spec-req))

;; Defines the contents of the nuvlabox-identifier template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :configurationTemplate here.
(s/def ::configurationTemplate
  (su/only-keys-maps ps/template-keys-spec
                     configuration-template-keys-spec-create))

(s/def ::nuvlabox-identifier-create
  (su/only-keys-maps ps/create-keys-spec
                     {:req-un [::configurationTemplate]}))
