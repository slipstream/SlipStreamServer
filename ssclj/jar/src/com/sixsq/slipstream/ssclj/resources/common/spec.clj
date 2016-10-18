(ns com.sixsq.slipstream.ssclj.resources.common.spec
  (:require
    [superstring.core :as str]
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; creates a closed map definition: only defined keys are permitted
;; (implementation provided on clojure mailing list by Alistair Roche)
;;
(defmacro only-keys
  [& {:keys [req req-un opt opt-un] :as args}]
  `(s/merge (s/keys ~@(apply concat (vec args)))
            (s/map-of ~(set (concat req
                                    (map (comp keyword name) req-un)
                                    opt
                                    (map (comp keyword name) opt-un)))
                      any?)))

;;
;; schema definitions for basic types
;;

(s/def ::nonblank-string (s/and string? (complement str/blank?)))

(s/def ::uri-token (s/and string? #(re-matches #"[\w\.\[\]\(\)\*\+\?\',:;/#@!$&=~-]+" %)))

(s/def ::timestamp (s/with-gen (s/and ::nonblank-string u/valid-timestamp?)
                               #(gen/fmap u/inst->timestamp (s/gen inst?))))

;;
;; schema definitions for common attributes
;;

(s/def ::href ::nonblank-string)
(s/def ::rel ::nonblank-string)
(s/def ::kw-or-str (s/or :keyword keyword? :string ::nonblank-string))

(s/def ::resource-link (s/keys :req-un [::href]))
(s/def ::resource-links (s/coll-of ::resource-link :min-count 1))

(s/def ::operation (only-keys :req-un [::href ::rel]))
(s/def ::operations (s/coll-of ::operation :min-count 1))

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

(s/def ::id ::nonblank-string)
(s/def ::resource-uri ::nonblank-string)
(s/def ::created ::timestamp)
(s/def ::updated ::timestamp)
(s/def ::name ::nonblank-string)
(s/def ::description ::nonblank-string)
(s/def ::common-attrs (s/keys :req-un [::id ::resource-uri ::created ::updated]
                              :opt-un [::name ::description ::properties ::operations]))

(def common-attrs {:req-un [::id ::resource-uri ::created ::updated]
                   :opt-un [::name ::description ::properties ::operations]})

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;
(s/def ::create-attrs (s/keys :req-un [::resource-uri]
                              :opt-un [::name ::description ::created ::updated ::properties ::operations]))

(def create-attrs {:req-un [::resource-uri]
                   :opt-un [::name ::description ::created ::updated ::properties ::operations]})

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; SlipStream implementation.
;;

(s/def ::type #{"USER" "ROLE"})
(s/def ::right #{"ALL" "VIEW" "MODIFY"})

(s/def ::principal ::nonblank-string)
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
(s/def ::displayName ::nonblank-string)
(s/def ::category ::nonblank-string)
(s/def ::mandatory boolean?)
(s/def ::readOnly boolean?)
(s/def ::order nat-int?)
(s/def ::enum (s/coll-of ::nonblank-str :min-count 1))
(s/def ::ParameterDescription (s/keys :req-un [::displayName
                                               :com.sixsq.slipstream.ssclj.resources.common.spec.alt/types]
                                      :opt-un [::category ::description ::mandatory ::readOnly ::order ::enum]))

(s/def ::resource-description (s/merge ::acl-attr (s/map-of keyword? ::ParameterDescription)))

