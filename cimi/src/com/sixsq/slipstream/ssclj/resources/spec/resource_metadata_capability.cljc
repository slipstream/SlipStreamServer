(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-capability
  "schema definitions for the 'capability' field of a ResourceMetadata resource"
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::name ::cimi-core/token)

(s/def ::uri ::cimi-core/uri)

(s/def ::description ::cimi-core/nonblank-string)

(s/def ::value any?)


(s/def ::capability (su/only-keys :req-un [::uri
                                           ::value]
                                  :opt-un [::name
                                           ::description]))

(s/def ::capabilities (s/coll-of ::capability :min-count 1 :type vector?))
