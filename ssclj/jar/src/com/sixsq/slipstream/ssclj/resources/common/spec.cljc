(ns com.sixsq.slipstream.ssclj.resources.common.spec
  (:require
    [clojure.string :as str]
    [clojure.spec :as s]
    [clojure.spec :as s]
    [clojure.spec :as s]))

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

(s/def ::port (s/int-in 1 65535))

;;
;; FIXME: Refine definition and provide reasonable generator.
;;
(s/def ::timestamp ::nonblank-string)
(s/def ::optional-timestamp (s/or :empty #{""} :not-empty ::timestamp)) ;; FIXME: This should not be needed.

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
(s/def ::resourceURI ::nonblank-string)
(s/def ::created ::timestamp)
(s/def ::updated ::timestamp)
(s/def ::name ::nonblank-string)
(s/def ::description ::nonblank-string)

;; FIXME: Needs to be made into a macro so that it can be included at compile time
#_(s/def ::common-attrs (s/keys :req-un [::id ::resourceURI ::created ::updated]
                              :opt-un [::name ::description ::properties ::operations]))

(def common-attrs {:req-un [::id ::resourceURI ::created ::updated]
                   :opt-un [::name ::description ::properties ::operations]})

;;
;; These are the common attributes for create resources.
;; All of the common attributes are allowed, but the optional
;; ones other than :name and :description will be ignored.
;;

;; FIXME: Needs to be made into a macro so that it can be included at compile time
#_(s/def ::create-attrs (s/keys :req-un [::resourceURI]
                              :opt-un [::name ::description ::created ::updated ::properties ::operations]))

(def create-attrs {:req-un [::resourceURI]
                   :opt-un [::name ::description ::created ::updated ::properties ::operations]})

;;
;; Ownership and access control
;;
;; These are additions to the standard CIMI schema for the
;; SlipStream implementation.
;;

(s/def ::principal ::nonblank-string)
(s/def ::type #{"USER" "ROLE"})
(s/def ::right #{"ALL" "VIEW" "MODIFY"})


(s/def ::owner (only-keys :req-un [::principal ::type]))

(s/def ::rule (only-keys :req-un [::principal ::type ::right]))
(s/def ::rules (s/coll-of ::rule :min-count 1))

(s/def ::acl (only-keys :req-un [::owner]
                        :opt-un [::rules]))
