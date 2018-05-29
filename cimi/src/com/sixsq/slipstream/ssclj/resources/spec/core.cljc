(ns com.sixsq.slipstream.ssclj.resources.spec.core
  "Spec definitions for basic types used in CIMI resources."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;;
;; basic types
;;

(s/def ::nonblank-string (s/and string? (complement str/blank?)))

(s/def ::text cu/as-text)

(defn token? [s] (re-matches #"^\S+$" s))
(s/def ::token (s/and string? token?))

(s/def ::port (s/int-in 1 65536))

;; FIXME: Provide an implementation that works with ClojureScript.
(s/def ::timestamp cu/as-datetime)

;; FIXME: Remove this definition when resources treat the timestamp as optional rather than allowing an empty value.
(s/def ::optional-timestamp (s/or :empty #{""} :not-empty ::timestamp))

;; FIXME: Replace this spec with one that enforces the URI grammar.
(s/def ::uri ::nonblank-string)

;; A username can only consist of letters, digits and underscores.
(s/def ::username (s/and string? #(re-matches #"^[a-zA-Z0-9_]+$" %)))

;; A kebab identifier consists of lowercased words separated by dashes.
(s/def ::kebab-identifier (s/and string? #(re-matches #"^[a-z]+(-[a-z]+)*$" %)))

;; A resource identifier consists of words of letters and digits separated
;; by underscores or dashes.
(s/def ::resource-identifier (s/and string? #(re-matches #"^[a-zA-Z0-9]+([_-][a-zA-Z0-9]+)*$" %)))

;; Words consisting of lowercase letters and digits, separated by dashes.
(s/def ::identifier (s/and string? #(re-matches #"^[a-z0-9]+(-[a-z0-9]+)*$" %)))

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

(def resource-href-regex #"^[a-z]([a-z-]*[a-z])?(/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?)?$")
(s/def ::resource-href (s/and string? #(re-matches resource-href-regex %)))

