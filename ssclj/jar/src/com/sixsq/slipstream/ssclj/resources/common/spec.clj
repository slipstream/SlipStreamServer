(ns com.sixsq.slipstream.ssclj.resources.common.spec
  (:require
    [superstring.core :as str]
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; schema definitions for basic types
;;

(s/def ::positive-int (s/and int? pos?))
(s/def ::nonnegative-int (s/and int? (complement neg?)))
(s/def ::port (s/int-in 0 65535))
(s/def ::blank-str (s/and string? str/blank?))
(s/def ::nonblank-str (s/and string? (complement str/blank?)))

(s/def ::nonblank-str-list (s/coll-of ::nonblank-str :min-count 1))

(s/def ::timestamp (s/and ::nonblank-str u/valid-timestamp?))

(s/def ::optional-timestamp (s/and string? (s/or :empty str/blank? :timestamp ::timestamp)))


;;
;; schema definitions for common attributes
;;

(s/def ::href ::nonblank-str)
(s/def ::resource-link (s/keys :req-un [::href]))
(s/def ::resource-links (s/coll-of ::resource-link :min-count 1))

(s/def ::rel ::nonblank-str)
(s/def ::operation (s/merge ::resource-link (s/keys :req-un [::rel])))
(s/def ::operations (s/coll-of ::operation :min-count 1))

(s/def ::kw-or-str (s/or :keyword keyword? :string string?))
(s/def ::properties (s/map-of ::kw-or-str string? :min-count 1))

;;
;; Common Attributes
;;

;;
;; These attributes are common to all resources except the
;; CloudEntryPoint.  When these attributes are passed into the
;; CIMI service implementation, the required entries and the
;; :operations will be replaced by the service-generated values.
;;

(s/def ::id ::nonblank-str)
(s/def ::resource-uri ::nonblank-str)
(s/def ::created ::timestamp)
(s/def ::updated ::timestamp)
(s/def ::name ::nonblank-str)
(s/def ::description ::nonblank-str)
(s/def ::common-attrs (s/keys :req-un [::id ::resource-uri ::created ::updated]
                              :opt-un [::name ::description ::properties ::operations]))

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(s/def ::create-attrs (s/keys :req-un [::resource-uri]
                              :opt-un [::name ::description ::created ::updated ::properties ::operations]))

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; StratusLab implementation.
;;

(s/def ::type #{"USER" "ROLE"})
(s/def ::right #{"ALL" "VIEW" "MODIFY"})

(s/def ::principal ::nonblank-str)
(s/def ::access-control-id (s/keys :req-un [::principal ::type]))

(s/def ::access-control-rule (s/merge ::access-control-id (s/keys :req-un [::right])))

(s/def ::owner ::access-control-id)
(s/def ::rules (s/coll-of ::access-control-rule :min-count 1))
(s/def ::acl (s/keys :req-un [::owner]
                     :opt-un [::rules]))

(s/def ::acl-attr (s/keys :req-un [::acl]))

;;
;; parameter descriptions
;;

(s/def :com.sixsq.slipstream.ssclj.resources.common.spec.alt/types #{"string" "boolean" "int" "float" "timestamp" "enum" "map" "list"})
(s/def ::displayName ::nonblank-str)
(s/def ::category ::nonblank-str)
(s/def ::mandatory boolean?)
(s/def ::readOnly boolean?)
(s/def ::order ::nonnegative-int)
(s/def ::enum ::nonblank-str-list)
(s/def ::ParameterDescription (s/keys :req-un [:displayName
                                               :com.sixsq.slipstream.ssclj.resources.common.spec.alt/types]
                                      :opt-un [:category :description :mandatory :readOnly :order :enum]))

(s/def ::resource-description (s/merge ::acl-attr (s/map-of keyword? ::ParameterDescription)))
