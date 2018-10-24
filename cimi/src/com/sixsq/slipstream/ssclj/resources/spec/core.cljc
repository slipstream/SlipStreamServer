(ns com.sixsq.slipstream.ssclj.resources.spec.core
  "Spec definitions for basic types used in CIMI resources."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [spec-tools.core :as st]))

;;
;; basic types
;;

(s/def ::scalar (s/or :string string?
                      :double double?
                      :integer int?
                      :boolean boolean?))

(s/def ::nonblank-string (s/and string? (complement str/blank?)))

(s/def ::text
  (st/spec {:spec                  cu/as-text
            :slipstream.es/mapping {:type "text"}}))

(defn token? [s] (re-matches #"^\S+$" s))
(s/def ::token (s/and string? token?))

(s/def ::port (s/int-in 1 65536))

;; FIXME: Provide an implementation that works with ClojureScript.
(s/def ::timestamp
  (st/spec {:spec                  cu/as-datetime
            :slipstream.es/mapping {:type   "date"
                                    :format "strict_date_optional_time||epoch_millis"}}))

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

;; A resource name is a Pascal case token.
(s/def ::resource-name (s/and string? #(re-matches #"^([A-Z]+[a-z]*)+$" %)))

;; Words consisting of lowercase letters and digits, separated by dashes.
(s/def ::identifier (s/and string? #(re-matches #"^[a-z0-9]+(-[a-z0-9]+)*$" %)))

(s/def ::resource-type ::kebab-identifier)

;; Email address verifier.
(def email-regex #"^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$")
(defn email? [s] (re-matches email-regex s))
(s/def ::email
  (s/and string? email?))

(def mimetype-regex #"[a-zA-Z0-9][a-zA-Z0-9!#$&^_-]{0,126}/[a-zA-Z0-9][a-zA-Z0-9!#$&^_-]{0,126}")
(defn mimetype? [s] (re-matches mimetype-regex s))
(s/def ::mimetype
  (s/and string? mimetype?))

;;
;; A resource href is the concatenation of a resource type and resource identifier separated
;; with a slash.  The later part is optional for singleton resources like the cloud-entry-point.
;;

(def resource-href-regex #"^[a-z]([a-z-]*[a-z])?(/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?)?$")
(s/def ::resource-href (s/and string? #(re-matches resource-href-regex %)))

