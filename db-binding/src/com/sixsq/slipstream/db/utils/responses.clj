(ns com.sixsq.slipstream.db.utils.responses
  "Utilities for generating common ring responses and exceptions."
  (:require
    [ring.util.response :as r]))


(defn json-content-type
  "Adds the body to a standard ring response and sets the content type to be
   JSON. The body must already be in JSON format or convertible to JSON."
  [body]
  (-> body
      (r/response)
      (r/content-type "application/json")))


(defn map-response
  "Returns a ring map with the given message, status, and resource id. Marks
   the response with a content type of JSON."
  [msg status id]
  (-> {:status      status
       :message     msg
       :resource-id id}
      (json-content-type)
      (r/status status)))


(defn ex-response
  "Creates an exception with the given mesage, status, and resource id. The
   content type is marked as JSON."
  [msg status id]
  (ex-info msg (map-response msg status id)))


(defn ex-not-found
  "Provides an exception with a 404 not found response and message."
  [id]
  (-> (str id " not found")
      (ex-response 404 id)))


(defn ex-conflict
  "Provides an exception with a 409 conflict response and message."
  [id]
  (-> (str "conflict with " id)
      (ex-response 409 id)))
