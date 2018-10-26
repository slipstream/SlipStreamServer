(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-attribute
  "schema definitions for the 'attributes' field of a ResourceMetadata resource"
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))


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
                                          ::uri
                                          ::displayName
                                          ::description
                                          ::help
                                          ::group
                                          ::category
                                          ::order
                                          ::hidden
                                          ::sensitive
                                          ::lines]))

(s/def ::attributes
  (st/spec {:spec                  (s/coll-of ::attribute :min-count 1 :type vector?)
            :slipstream.es/mapping {:type "object", :enabled false}}))
