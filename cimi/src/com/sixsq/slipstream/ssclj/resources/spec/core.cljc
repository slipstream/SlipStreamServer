(ns com.sixsq.slipstream.ssclj.resources.spec.core
  "Spec definitions for basic types used in CIMI resources."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [spec-tools.core :as st]))


(s/def ::scalar
  (-> (st/spec (s/or :string string?
                     :double double?
                     :integer int?
                     :boolean boolean?))
      (assoc :name "scalar"
             :json-schama/description "valid scalar value for JSON")))


(s/def ::nonblank-string
  (-> (st/spec (s/and string? (complement str/blank?)))
      (assoc :name "non-blank string"
             :type :string
             :json-schema/description "string containing something other than only whitespace"
             :json-schema/type "string")))


(s/def ::text
  (-> (st/spec cu/as-text)
      (assoc :name "text"
             :type :string
             :json-schema/description "text containing something other than only whitespace"
             :json-schema/type "string"

             :slipstream.es/mapping {:type "text"})))


(defn token? [s] (re-matches #"^\S+$" s))
(s/def ::token
  (-> (st/spec (s/and string? token?))
      (assoc :name "token"
             :type :string
             :json-schema/description "a sequence of one or more non-whitespace characters"
             :json-schema/type "string")))


(s/def ::port
  (-> (st/spec (s/int-in 1 65536))
      (assoc :name "port"
             :type :long
             :json-schema/description "port number in the range 1 to 65535"
             :json-schema/type "integer")))


;; FIXME: Provide an implementation that works with ClojureScript.
(s/def ::timestamp
  (-> (st/spec cu/as-datetime)
      (assoc :name "timestamp"
             :type :string
             :json-schema/description "UTC timestamp"
             :json-schema/type "dateTime"
             :slipstream.es/mapping {:type   "date"
                                     :format "strict_date_optional_time||epoch_millis"})))


;; FIXME: Replace this spec with one that enforces the URI grammar.
(s/def ::uri
  (-> (st/spec ::nonblank-string)
      (assoc :name "URI"
             :type :uri
             :json-schema/description "Uniform Resource Identifier"
             :json-schema/type "string")))


(s/def ::username
  (-> (st/spec (s/and string? #(re-matches #"^[a-zA-Z0-9_]+$" %)))
      (assoc :name "username"
             :type :string
             :json-schema/description "string consisting only of letters, digits, and underscores"
             :json-schema/type "string")))


(s/def ::kebab-identifier
  (-> (st/spec (s/and string? #(re-matches #"^[a-z]+(-[a-z]+)*$" %)))
      (assoc :name "kebab-identifier"
             :type :string
             :json-schema/description "string consisting of lowercased words separated by dashes"
             :json-schema/type "string")))


(s/def ::resource-identifier
  (-> (st/spec (s/and string? #(re-matches #"^[a-zA-Z0-9]+([_-][a-zA-Z0-9]+)*$" %)))
      (assoc :name "resource identifier"
             :type :string
             :json-schema/description "string consisting of letters and digits separated by single underscores or dashes"
             :json-schema/type "string")))


(s/def ::resource-name
  (-> (st/spec (s/and string? #(re-matches #"^([A-Z]+[a-z]*)+$" %)))
      (assoc :name "resource name"
             :type :string
             :json-schema/description "CIMI resource name (Pascal case)"
             :json-schema/type "string")))


(s/def ::identifier
  (-> (st/spec (s/and string? #(re-matches #"^[a-z0-9]+(-[a-z0-9]+)*$" %)))
      (assoc :name "identifier"
             :type :string
             :json-schema/description "string consisting of words of lowercase letters and digits separated by single dashes"
             :json-schema/type "string")))


(s/def ::resource-type
  (-> (st/spec ::kebab-identifier)
      (assoc :name "resource type"
             :type :string
             :json-schema/description "CIMI resource type (kebab case)"
             :json-schema/type "string")))


(def email-regex #"^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$")
(defn email? [s] (re-matches email-regex s))
(s/def ::email
  (-> (st/spec (s/and string? email?))
      (assoc :name "email"
             :type :string
             :json-schema/description "valid email address"
             :json-schema/type "string")))


(def mimetype-regex #"[a-zA-Z0-9][a-zA-Z0-9!#$&^_-]{0,126}/[a-zA-Z0-9][a-zA-Z0-9!#$&^_-]{0,126}")
(defn mimetype? [s] (re-matches mimetype-regex s))
(s/def ::mimetype
  (-> (st/spec (s/and string? mimetype?))
      (assoc :name "mimetype"
             :type :string
             :json-schema/description "document mimetype"
             :json-schema/type "string")))

;;
;; A resource href is the concatenation of a resource type and resource identifier separated
;; with a slash.  The later part is optional for singleton resources like the cloud-entry-point.
;;

(def resource-href-regex #"^[a-z]([a-z-]*[a-z])?(/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?)?$")
(s/def ::resource-href
  (-> (st/spec (s/and string? #(re-matches resource-href-regex %)))
      (assoc :name "resource href"
             :type :string
             :json-schema/description "concatenation of a resource type and resource identifier separated with a slash"
             :json-schema/type "ref")))
