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

(s/def ::nonblank-string
  (-> (st/spec (s/and string? (complement str/blank?)))
      (assoc :name "non-blank string"
             :description "a string containing something other than only whitespace"
             :type :string)))


(s/def ::text
  (-> (st/spec cu/as-text)
      (assoc :name "text"
             :description "text containing something other than only whitespace"
             :type :string)))


(defn token? [s] (re-matches #"^\S+$" s))
(s/def ::token
  (-> (st/spec (s/and string? token?))
      (assoc :name "token"
             :description "a sequence of one or more non-whitespace characters"
             :type :string)))


(s/def ::port
  (-> (st/spec (s/int-in 1 65536))
      (assoc :name "port"
             :description "port number in the range 1 to 65535"
             :type :integer)))

;; FIXME: Provide an implementation that works with ClojureScript.
(s/def ::timestamp
  (st/spec {:spec                  cu/as-datetime
            :slipstream.es/mapping {:type   "date"
                                    :format "strict_date_optional_time||epoch_millis"}}))

;; FIXME: Remove this definition when resources treat the timestamp as optional rather than allowing an empty value.
(s/def ::optional-timestamp (s/or :empty #{""} :not-empty ::timestamp))

;; FIXME: Replace this spec with one that enforces the URI grammar.
(s/def ::uri
  (-> (st/spec ::nonblank-string)
      (assoc :name "URI"
             :description "Uniform Resource Identifier"
             :type :string)))

(s/def ::username
  (-> (st/spec (s/and string? #(re-matches #"^[a-zA-Z0-9_]+$" %)))
      (assoc :name "username"
             :description "string consisting only of letters, digits, and underscores"
             :type :string)))

(s/def ::kebab-identifier
  (-> (st/spec (s/and string? #(re-matches #"^[a-z]+(-[a-z]+)*$" %)))
      (assoc :name "kebab-identifier"
             :description "string consisting of lowercased words separated by dashes"
             :type :string)))

(s/def ::resource-identifier
  (-> (st/spec (s/and string? #(re-matches #"^[a-zA-Z0-9]+([_-][a-zA-Z0-9]+)*$" %)))
      (assoc :name "resource identifier"
             :description "string consisting of letters and digits separated by single underscores or dashes"
             :type :string)))

;; A resource name is a Pascal case token.
(s/def ::resource-name (s/and string? #(re-matches #"^([A-Z]+[a-z]*)+$" %)))

;; Words consisting of lowercase letters and digits, separated by dashes.
(s/def ::identifier
  (-> (st/spec (s/and string? #(re-matches #"^[a-z0-9]+(-[a-z0-9]+)*$" %)))
      (assoc :name "identifier"
             :description "string consisting of words of lowercase letters and digits separated by single dashes"
             :type :string)))

(s/def ::resource-type ::kebab-identifier)


(def email-regex #"^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$")
(defn email? [s] (re-matches email-regex s))
(s/def ::email
  (-> (st/spec (s/and string? email?))
      (assoc :name "email"
             :description "string containing a valid email address"
             :type :string)))

(def mimetype-regex #"[a-zA-Z0-9][a-zA-Z0-9!#$&^_-]{0,126}/[a-zA-Z0-9][a-zA-Z0-9!#$&^_-]{0,126}")
(defn mimetype? [s] (re-matches mimetype-regex s))
(s/def ::mimetype
  (s/and string? mimetype?))

;;
;; A resource href is the concatenation of a resource type and resource identifier separated
;; with a slash.  The later part is optional for singleton resources like the cloud-entry-point.
;;

(def resource-href-regex #"^[a-z]([a-z-]*[a-z])?(/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?)?$")
(s/def ::resource-href
  (-> (st/spec (s/and string? #(re-matches resource-href-regex %)))
      (assoc :name "resource HREF"
             :description "concatenation of a resource type and resource identifier separated with a slash")))

