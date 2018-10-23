(ns com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-action
  "schema definitions for the 'actions' field of a ResourceMetadata resource"
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(s/def ::name ::cimi-core/token)

(s/def ::uri ::cimi-core/uri)

(s/def ::description ::cimi-core/nonblank-string)

;; only those methods typically used in REST APIs are permitted by this implementation
(s/def ::method #{"GET" "POST" "PUT" "DELETE"})

(s/def ::inputMessage ::cimi-core/mimetype)

(s/def ::outputMessage ::cimi-core/mimetype)

(s/def ::action (su/only-keys :req-un [::name
                                       ::uri
                                       ::method
                                       ::inputMessage
                                       ::outputMessage]
                              :opt-un [::description]))

(s/def ::actions (s/coll-of ::action :min-count 1 :type vector?))
