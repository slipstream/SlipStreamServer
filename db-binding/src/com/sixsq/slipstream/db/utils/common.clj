(ns com.sixsq.slipstream.db.utils.common
  "General utilities for dealing with resources."
  (:require
    [clojure.tools.logging :as log]
    [clojure.edn :as edn]
    [superstring.core :as s]
    [clojure.string :as str])
  (:import
    [javax.xml.bind DatatypeConverter]))

;; NOTE: this cannot be replaced with s/lisp-case because it
;; will treat a '/' in a resource name as a word separator.
(defn de-camelcase [str]
  (str/join "-" (map str/lower-case (str/split str #"(?=[A-Z])"))))


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
    (s/pascal-case s)))


(defn split-id
  "Split resource ID into a tuple of [collection docid]. The id is usually in
  the form collection/docid. The CloudEntryPoint is an exception where there is
  no docid, so this will return the collection as the docid: [collection
  collection]. If the input is nil, then the function will return nil."
  [id]
  (when id
    (let [[type docid] (str/split id #"/")]
      [type (or docid type)])))


(defn split-id-kw
  "Same as `split-id` exception that the collection ID will be returned as a
   keyword."
  [id]
  (when id
    (let [[type docid] (str/split id #"/")]
      [(keyword type) (or docid type)])))
