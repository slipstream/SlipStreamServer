(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-attribute
  "schema definitions for the 'attributes' field of a ResourceMetadata resource"
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::name ::cimi-core/token)

(s/def ::namespace ::cimi-core/uri)

(s/def ::uri ::cimi-core/uri)

(s/def ::type #{"string" "text" "boolean" "uri" "integer" "float" "double"
                "timestamp" "enum" "password" "hidden" "object" "array"})

(s/def ::providerMandatory boolean?)

(s/def ::consumerMandatory boolean?)

(s/def ::mutable boolean?)

(s/def ::consumerWritable boolean?)


;;
;; the following attributes are extensions to the standard that are
;; useful for rendering forms for browser-based clients
;;

(s/def ::description ::cimi-core/nonblank-string)

(s/def ::help ::cimi-core/nonblank-string)

(s/def ::displayName ::cimi-core/nonblank-string)

(s/def ::category ::cimi-core/nonblank-string)

(s/def ::order nat-int?)

(s/def ::enum (s/coll-of ::cimi-core/nonblank-string :min-count 1))


;;
;; NOTE: The CIMI specification states that the :type attributes will
;; not be present for standard CIMI attributes.  This implementation
;; makes :type mandatory for all attribute descriptions.  This makes
;; life easier for clients.
;;
(s/def ::attribute (su/only-keys :req-un [::name
                                          ::type
                                          ::providerMandatory
                                          ::consumerMandatory
                                          ::mutable
                                          ::consumerWritable]
                                 :opt-un [::namespace
                                          ::description
                                          ::help
                                          ::displayName
                                          ::category
                                          ::order
                                          ::enum]))

(s/def ::attributes (s/coll-of ::attribute :min-count 1 :type vector?))
