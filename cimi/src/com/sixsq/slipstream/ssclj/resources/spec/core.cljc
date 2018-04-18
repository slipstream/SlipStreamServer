(ns com.sixsq.slipstream.ssclj.resources.spec.core
  "Spec definitions for basic types used in CIMI resources."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]))

;;
;; basic types
;;

(s/def ::nonblank-string (s/and string? (complement str/blank?)))

(defn token? [s] (re-matches #"^\S+$" s))
(s/def ::token (s/and string? token?))

(s/def ::port (s/int-in 1 65536))

;; FIXME: Provide an implementation that works with ClojureScript.
(s/def ::timestamp (s/with-gen (s/and string? cu/as-datetime)
                               (constantly (gen/fmap cu/unparse-timestamp-date (gen/gen-for-pred inst?)))))

;; FIXME: Remove this definition when resources treat the timestamp as optional rather than allowing an empty value.
(s/def ::optional-timestamp (s/or :empty #{""} :not-empty ::timestamp))

;; FIXME: Replace this spec with one that enforces the URI grammar.
(s/def ::uri ::nonblank-string)

;; A username can only consist of letters, digits and underscores.
(s/def ::username
  (su/regex-string #"[a-zA-Z0-9_]" #"^[a-zA-Z0-9_]+$"))

;; A kebab identifier consists of lowercased words separated by dashes.
(s/def ::kebab-identifier
  (su/regex-string #"[a-z-]" #"^[a-z]+(-[a-z]+)*$"))

;; A resource identifier consists of words of letters and digits separated
;; by underscores or dashes.
(s/def ::resource-identifier
  (su/regex-string #"[a-zA-Z0-9_-]" #"^[a-zA-Z0-9]+([_-][a-zA-Z0-9]+)*$"))

;; Words consisting of lowercase letters and digits, separated by dashes.
(s/def ::identifier
  (su/regex-string #"[a-z0-9-]" #"^[a-z0-9]+(-[a-z0-9]+)*$"))

(s/def ::resource-type ::kebab-identifier)

;; Email address verifier.
(def email-regex #"^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$")
(defn email? [s] (re-matches email-regex s))
(s/def ::email
  (s/and string? email?))

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
(s/def ::resource-href
  (s/with-gen (s/and string? #(re-matches resource-href-regex %))
              (constantly (gen/fmap join-href-parts
                                    (gen/tuple
                                      (s/gen ::kebab-identifier)
                                      (gen/frequency [[95 (s/gen ::resource-identifier)]
                                                      [5 (s/gen #{nil})]]))))))

