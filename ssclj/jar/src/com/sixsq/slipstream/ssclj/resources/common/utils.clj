(ns com.sixsq.slipstream.ssclj.resources.common.utils
  "General utilities for dealing with resources."
  (:require
    [clojure.tools.logging :as log]
    [clojure.edn :as edn]
    [superstring.core :as str]
    [clj-time.core :as time]
    [clj-time.format :as time-fmt]
    [clojure.spec.alpha :as s]
    [ring.util.response :as r]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]
    [clojure.data.json :as json]
    [clj-time.coerce :as c]
    [ring.util.codec :as codec]
    [com.sixsq.slipstream.ssclj.util.log :as log-util])
  (:import
    [java.util List Map UUID Date]
    [javax.xml.bind DatatypeConverter]))

(defn string->int [s]
  (when (re-matches #"\d+" s)
    (read-string s)))

;; NOTE: this cannot be replaced with s/lisp-case because it
;; will treat a '/' in a resource name as a word separator.
(defn de-camelcase [s]
  (if s
    (str/join "-" (map str/lower-case (str/split s #"(?=[A-Z])")))
    ""))

;;
;; utilities for generating ring responses for standard
;; conditions and ex-info exceptions with these responses
;; embedded in them
;;

(defn response-created
  "Provides a created response (201) with the Location header given by the
   identifier and provides the Set-Cookie header with the given cookie, if
   the cookie value is not nil."
  [id & [[cookie-name cookie]]]
  (cond-> {:status 201, :headers {"Location" id}}
          cookie (assoc :cookies {cookie-name cookie})))

(defn response-final-redirect
  "Provides a created response (303) with the Location header given by the
   identifier and provides the Set-Cookie header with the given cookie, if
   the cookie value is not nil."
  [location & [[cookie-name cookie]]]
  (cond-> {:status 303, :headers {"Location" location}}
          cookie (assoc :cookies {cookie-name cookie})))

(defn json-response
  [body]
  (-> body
      (r/response)
      (r/content-type "application/json")))

(defn map-response
  ([msg status]
   (map-response msg status nil nil))
  ([msg status id]
   (map-response msg status id nil))
  ([msg status id location]
   (let [resp (-> (cond-> {:status status, :message msg}
                          id (assoc :resource-id id))
                  json-response
                  (r/status status))]
     (if location
       (update-in resp [:headers "Location"] (constantly location))
       resp))))

(defn ex-response
  ([msg status]
   (ex-info msg (map-response msg status)))
  ([msg status id]
   (ex-info msg (map-response msg status id)))
  ([msg status id location]
   (ex-info msg (map-response msg status id location))))

(defn ex-not-found
  [id]
  (let [msg (str id " not found")]
    (ex-response msg 404 id)))

(defn ex-conflict
  [id]
  (let [msg (str "conflict with " id)]
    (ex-response msg 409 id)))

(defn ex-unauthorized
  [id]
  (let [msg (str "invalid credentials for '" id "'")]
    (ex-response msg 403 id)))

(defn ex-bad-method
  [{:keys [uri request-method] :as request}]
  (ex-response
    (str "invalid method (" (name request-method) ") for " uri)
    405 uri))

(defn ex-bad-action
  [{:keys [uri request-method] :as request} action]
  (ex-response
    (str "undefined action (" (name request-method) ", " action ") for " uri)
    404 uri))

(defn ex-bad-CIMI-filter
  [parse-failure]
  (ex-response (str "Invalid CIMI filter. " (prn-str parse-failure)) 400))

(defn ex-redirect
  "Provides an exception that will redirect (303) to the given redirectURI, by
   setting the Location header. The message is added as an 'error' query
   parameter to the redirectURI."
  [msg id redirectURI]
  (let [query (str "?error=" (codec/url-encode msg))]
    (ex-response msg 303 id (str redirectURI query))))

;;
;; resource ID utilities
;;

(defn random-uuid
  "Provides the string representation of a pseudo-random UUID."
  []
  (str (UUID/randomUUID)))

(defn new-resource-id
  [resource-name]
  (str resource-name "/" (random-uuid)))

(defn resource-name
  [resource-id]
  (-> resource-id
      (str/split #"/")
      first))

(defn split-resource-id
  "Provide a tuple of [type docid] for a resource ID.  For IDs
   that don't have an identifier part (e.g. the CloudEntryPoint),
   a single element vector will be returned."
  [id]
  (let [[type docid] (str/split id #"/")]
    [type (or docid type)]))

;;
;; utilities for handling common attributes
;;

(defn strip-common-attrs
  "Strips all common resource attributes from the map."
  [m]
  (dissoc m :id :name :description :created :updated :properties))

(defn strip-service-attrs
  "Strips common attributes from the map whose values are controlled
   entirely by the service.  These include :id, :created, :updated,
   :resourceURI, and :operations."
  [m]
  (dissoc m :id :created :updated :resourceURI :operations))

(defn update-timestamps
  "Sets the updated attribute and optionally the created attribute
   in the request.  The created attribute is only set if the existing value
   is missing or evaluates to false."
  [data]
  (let [updated (time-fmt/unparse (:date-time time-fmt/formatters) (time/now))
        created (or (:created data) updated)]
    (assoc data :created created :updated updated)))

(defn unparse-timestamp
  "Returns the string representation of the given timestamp."
  [^Date timestamp]
  (try
    (time-fmt/unparse (:date-time time-fmt/formatters) (c/from-date timestamp))
    (catch Exception _
      nil)))

(defn parse-timestamp
  "Tries to parse the given string as a DateTime value.  Returns the DateTime
   instance on success and nil on failure."
  [data]
  (try
    (time-fmt/parse (:date-time time-fmt/formatters) data)
    (catch Exception _
      nil)))

(defn log-and-throw-400
  "Logs the given message as a warning and then throws an exception with a
   400 response."
  [msg]
  (let [response (-> {:status 400 :message msg}
                     json-response
                     (r/status 400))]
    (log/warn msg)
    (throw (ex-info msg response))))

(defn create-spec-validation-fn
  "Creates a validation function that compares a resource against the
   given schema.  The generated function raises an exception with the
   violations of the schema and a 400 ring response. If everything's
   OK, then the resource itself is returned."
  [spec]
  (let [ok? (partial s/valid? spec)
        explain (partial s/explain-str spec)]
    (fn [resource]
      (if-not (ok? resource)
        (log-util/log-and-throw-400 (str "resource does not satisfy defined schema: " (explain resource)))
        resource))))

(defn encode-base64
  "Encodes a clojure value or data structure (EDN) into a base64
   string representation."
  [m]
  (-> m
      (pr-str)
      (.getBytes)
      (DatatypeConverter/printBase64Binary)))

(defn decode-base64
  "Decodes a base64 string representation of a clojure value or
   data structure (EDN) into a clojure value."
  [b64]
  (-> b64
      (DatatypeConverter/parseBase64Binary)
      (String.)
      (edn/read-string)))

(defn- clojurify
  [exp]
  (cond
    (instance? Map exp) (into {} (map (fn [[k v]] [(keyword k) v]) exp))
    (instance? List exp) (vec exp)
    :else exp))

(defn walk-clojurify
  [java-map]
  (clojure.walk/prewalk clojurify java-map))

(defn into-vec-without-nil
  [op xs]
  (when (->> xs
             (remove nil?)
             seq)
    (->> (into [op] xs)
         (remove nil?)
         vec)))

(defn- lisp-cased?
  [s]
  (re-matches #"[a-z]+(-[a-z]+)*" s))

(defn lisp-to-camelcase
  "Converts s to CamelCase format.
  s must be lisp-cased, if not empty string is returned."
  [s]
  (if-not (lisp-cased? s)
    (do
      (log/warn s " is not lisp-cased.")
      "")
    (str/pascal-case s)))

(defn map-multi-line
  [m]
  (str "\n" (clojure.pprint/write m :stream nil :right-margin 50)))

(defn- name-plus-namespace
  [kw]
  (if (keyword? kw)
    (subs (str kw) 1)
    (name kw)))

(defn serialize
  [resource]
  (with-out-str
    (json/pprint resource :key-fn name-plus-namespace)))

(defn deserialize
  [s]
  (json/read-str s :key-fn keyword))

