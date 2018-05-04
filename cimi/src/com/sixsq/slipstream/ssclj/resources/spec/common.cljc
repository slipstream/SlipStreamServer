(ns com.sixsq.slipstream.ssclj.resources.spec.common
  "Spec definitions for common types used in CIMI resources."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.acl :as cimi-acl]
    [com.sixsq.slipstream.ssclj.resources.spec.common-operation :as cimi-common-operation]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;;
;; common CIMI attributes
;;

;; simple attributes
(s/def ::id ::cimi-core/resource-href)
(s/def ::resourceURI ::cimi-core/uri)
(s/def ::created ::cimi-core/timestamp)
(s/def ::updated ::cimi-core/timestamp)
(s/def ::name ::cimi-core/nonblank-string)
(s/def ::description ::cimi-core/text)

;; links between resources
(s/def ::href ::cimi-core/resource-href)
(s/def ::resource-link (s/keys :req-un [::href]))
(s/def ::resource-links (s/coll-of ::resource-link :min-count 1))

;; resource operations
(s/def ::operation (su/only-keys :req-un [::cimi-common-operation/href
                                          ::cimi-common-operation/rel]))
(s/def ::operations (s/coll-of ::operation :min-count 1))

;; client-controlled properties
(s/def ::kw-or-str (s/or :keyword keyword? :string ::cimi-core/nonblank-string))
(s/def ::properties (s/map-of ::kw-or-str string? :min-count 1))

(s/def ::acl (su/only-keys :req-un [::cimi-acl/owner]
                           :opt-un [::cimi-acl/rules]))

(def ^:const common-attrs
  "clojure.spec/keys specification (as a map) for common CIMI attributes
   for regular resources"
  {:req-un [::id
            ::resourceURI
            ::created
            ::updated
            ::acl]
   :opt-un [::name
            ::description
            ::properties
            ::operations]})

(def ^:const create-attrs
  "clojure.spec/keys specification (as a map) for common CIMI attributes
   for the 'create' resources used when creating resources from a template.
   This applies to the create wrapper and not the embedded resource
   template!"
  {:req-un [::resourceURI]
   :opt-un [::name
            ::description
            ::created
            ::updated
            ::properties
            ::operations
            ::acl]})

(def ^:const template-attrs
  "The clojure.spec/keys specification (as a map) for common CIMI attributes
   for the resource templates that are embedded in 'create' resources. Although
   these may be added to the templates (usually by reference), they will have
   no affect on the created resource."
  {:opt-un [::resourceURI
            ::name
            ::description
            ::created
            ::updated
            ::properties
            ::operations
            ::acl]})
