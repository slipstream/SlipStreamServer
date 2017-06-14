(ns com.sixsq.slipstream.ssclj.resources.spec.common
  "Spec definitions for basic types and common types used in CIMI
   resources."
  (:require
    [clojure.set :as set]
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]))

;;
;; basic types
;; keep all these definitions in the 'cimi.core' namespace
;;

(s/def :cimi.core/nonblank-string (s/and string? (complement str/blank?)))

(defn token? [s] (re-matches #"^\S+$" s))
(s/def :cimi.core/token (s/and string? token?))

(s/def :cimi.core/port (s/int-in 1 65536))

;; FIXME: Provide an implementation that works with ClojureScript.
(s/def :cimi.core/timestamp (s/with-gen (s/and string? cu/parse-timestamp)
                                        (constantly (gen/fmap cu/unparse-timestamp (gen/gen-for-pred inst?)))))

;; FIXME: Remove this definition when resources treat the timestamp as optional rather than allowing an empty value.
(s/def :cimi.core/optional-timestamp (s/or :empty #{""} :not-empty :cimi.core/timestamp))

;; FIXME: Replace this spec with one that enforces the URI grammar.
(s/def :cimi.core/uri :cimi.core/nonblank-string)

;; A username can only consist of letters, digits and underscores.
(s/def :cimi.core/username
  (su/regex-string #"[a-zA-Z0-9_]" #"^[a-zA-Z0-9_]+$"))

;; A kebab identifier consists of lowercased words separated by dashes.
(s/def :cimi.core/kebab-identifier
  (su/regex-string #"[a-z-]" #"^[a-z]+(-[a-z]+)*$"))

;; A resource identifier consists of words of letters and digits separated
;; by underscores or dashes.
(s/def :cimi.core/resource-identifier
  (su/regex-string #"[a-zA-Z0-9_-]" #"^[a-zA-Z0-9]+([_-][a-zA-Z0-9]+)*$"))

;; Words consisting of lowercase letters and digits, separated by dashes.
(s/def :cimi.core/identifier
  (su/regex-string #"[a-z0-9-]" #"^[a-z0-9]+(-[a-z0-9]+)*$"))

(s/def :cimi.core/resource-type :cimi.core/kebab-identifier)

;;
;; A resource href is the concatenation of a resource type and resource identifier separated
;; with a slash.  The later part is optional for singleton resources like the cloud-entry-point.
;;
(defn- join-href-parts
  [[resource-type resource-identifier]]
  (if resource-identifier
    (str resource-type "/" resource-identifier)
    resource-type))

(def resource-href-regex #"^[a-z]([a-z-]*[a-z])?(/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?)?$")
(s/def :cimi.core/resource-href
  (s/with-gen (s/and string? #(re-matches resource-href-regex %))
              (constantly (gen/fmap join-href-parts
                                    (gen/tuple
                                      (s/gen :cimi.core/kebab-identifier)
                                      (gen/frequency [[95 (s/gen :cimi.core/resource-identifier)]
                                                      [5 (s/gen #{nil})]]))))))

;;
;; common CIMI attributes
;; all these definitions are the 'cimi.common' namespace
;; sub-attributes are in child namespaces of 'cimi.common'
;;

;; simple attributes
(s/def :cimi.common/id :cimi.core/resource-href)
(s/def :cimi.common/resourceURI :cimi.core/uri)
(s/def :cimi.common/created :cimi.core/timestamp)
(s/def :cimi.common/updated :cimi.core/timestamp)
(s/def :cimi.common/name :cimi.core/nonblank-string)
(s/def :cimi.common/description :cimi.core/nonblank-string)

;; links between resources
(s/def :cimi.common.link/href :cimi.core/resource-href)
(s/def :cimi.common/resource-link (s/keys :req-un [:cimi.common.link/href]))
(s/def :cimi.common/resource-links (s/coll-of :cimi.common/resource-link :min-count 1))

;; resource operations
(s/def :cimi.common.operation/href :cimi.core/nonblank-string)
(s/def :cimi.common.operation/rel :cimi.core/nonblank-string)
(s/def :cimi.common/operation (su/only-keys :req-un [:cimi.common.operation/href
                                                     :cimi.common.operation/rel]))
(s/def :cimi.common/operations (s/coll-of :cimi.common/operation :min-count 1))

;; client-controlled properties
(s/def :cimi.common/kw-or-str (s/or :keyword keyword? :string :cimi.core/nonblank-string))
(s/def :cimi.common/properties (s/map-of :cimi.common/kw-or-str string? :min-count 1))


;;
;; Access Control Lists (an extension to the CIMI standard)
;; these definitions are the 'cimi.acl' namespace
;;

;; A principal consists of words containing letters and digits, separated by
;; underscores, dashes, slashes, or dots.
(s/def :cimi.acl/principal
  (su/regex-string #"[a-zA-Z0-9/\._-]" #"^[a-zA-Z0-9]+([/\._-][a-zA-Z0-9]+)*$"))

(s/def :cimi.acl/type #{"USER" "ROLE"})
(s/def :cimi.acl/right #{"ALL" "VIEW" "MODIFY"})

(s/def :cimi.acl/owner (su/only-keys :req-un [:cimi.acl/principal
                                              :cimi.acl/type]))

(s/def :cimi.acl/rule (su/only-keys :req-un [:cimi.acl/principal
                                             :cimi.acl/type
                                             :cimi.acl/right]))
(s/def :cimi.acl/rules (s/coll-of :cimi.acl/rule :min-count 1))

(s/def :cimi.common/acl (su/only-keys :req-un [:cimi.acl/owner]
                                      :opt-un [:cimi.acl/rules]))

(def ^:const common-attrs
  "clojure.spec/keys specification (as a map) for common CIMI attributes
   for regular resources"
  {:req-un [:cimi.common/id
            :cimi.common/resourceURI
            :cimi.common/created
            :cimi.common/updated
            :cimi.common/acl]
   :opt-un [:cimi.common/name
            :cimi.common/description
            :cimi.common/properties
            :cimi.common/operations]})

(def ^:const create-attrs
  "clojure.spec/keys specification (as a map) for common CIMI attributes
   for the 'create' resources used when creating resources from a template.
   This applies to the create wrapper and not the embedded resource
   template!"
  {:req-un [:cimi.common/resourceURI]
   :opt-un [:cimi.common/name
            :cimi.common/description
            :cimi.common/created
            :cimi.common/updated
            :cimi.common/properties
            :cimi.common/operations
            :cimi.common/acl]})

(def ^:const template-attrs
  "The clojure.spec/keys specification (as a map) for common CIMI attributes
   for the resource templates that are embedded in 'create' resources. Although
   these may be added to the templates (usually by reference), they will have
   no affect on the created resource."
  {:opt-un [:cimi.common/resourceURI
            :cimi.common/name
            :cimi.common/description
            :cimi.common/created
            :cimi.common/updated
            :cimi.common/properties
            :cimi.common/operations
            :cimi.common/acl]})
