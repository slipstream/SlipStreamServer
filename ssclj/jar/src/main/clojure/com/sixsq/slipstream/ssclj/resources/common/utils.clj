(ns com.sixsq.slipstream.ssclj.resources.common.utils
  "General utilities for dealing with resources."
  (:require
    [clojure.tools.logging    :as log]
    [clojure.edn              :as edn]
    [clj-time.core            :as time]
    [clj-time.format          :as time-fmt]
    [schema.core              :as s]
    [ring.util.response       :as r])
  (:import
    [java.util UUID]
    [javax.xml.bind DatatypeConverter]))

;;
;; utilities for generating ring responses for standard
;; conditions and ex-info exceptions with these responses
;; embedded in them
;;

(defn json-response
  [body]
  (-> body
      (r/response)
      (r/content-type "application/json")))

(defn map-response 
  [msg status id]
  (-> {:status      status
       :message     msg
       :resource-id id}
      (json-response)
      (r/status status)))

(defn ex-response 
  [msg status id]
  (ex-info msg (map-response msg status id)))

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
  (let [msg (str "not authorized for '" id "'")]
    (ex-response msg 403 id)))

(defn ex-bad-method
  [{:keys [uri request-method] :as request}]
  (-> (str "invalid method (" (name request-method) ") for " uri)    
      (ex-response 405 uri)))

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

(defn valid-timestamp?
  "Tries to parse the given string as a DateTime value.  Returns the DateTime
   instance on success and nil on failure."
  [data]
  (time-fmt/parse (:date-time time-fmt/formatters) data))

(defn valid-number?
  [s]
  (number? (read-string s)))

(defn create-validation-fn
  "Creates a validation function that compares a resource against the
   given schema.  The generated function raises an exception with the
   violations of the schema and a 400 ring response. If everything's
   OK, then the resource itself is returned."
   [schema]
   (let [checker (s/checker schema)]
    (fn [resource]
      (if-let [msg (checker resource)]        
        (let [msg (str "resource does not satisfy defined schema: " msg)
          response (->  {:status  400 :message msg}
                        json-response
                        (r/status 400))]
          (log/warn msg)
          (throw (ex-info msg response)))
        resource))))

(defn decrypt
  "This function should eventually decrypt the given value based on
   one or more public keys.  Currently a no-op that just returns the
   given value."
  [value]
  value)

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

