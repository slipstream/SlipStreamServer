(ns com.sixsq.slipstream.auth.utils.timestamp
  "Utilities for creating expiration times for token claims and for formatting
   them correctly for the cookie 'expires' field."
  (:require
    [com.sixsq.slipstream.auth.utils.config :as cf]
    [clj-time.core :as t]
    [clj-time.coerce :as c]
    [clj-time.format :as f]))

(def default-lifetime-minutes (* 12 60))

(def cookie-date-formatter (f/formatter "EEE, dd MMM yyyy HH:mm:ss z"))

(defn format-timestamp
  "Returns a timestamp formatted in cookie date format from the number
   of **seconds** since the epoch."
  [timestamp]
  (f/unparse cookie-date-formatter (c/from-long (* 1000 timestamp))))

(defn expiry-later
  "Returns the expiry timestamp as the number of **seconds** since the epoch.
   If n is provided, then the expiry timestamp corresponds to n minutes later.
   If it is not provided, then the default lifetime is used."
  [& [n]]
  (let [n (or n (expiry-later (cf/property-value :token-nb-minutes-expiry default-lifetime-minutes)))]
    (-> n t/minutes t/from-now c/to-long (quot 1000))))

(defn formatted-expiry-later
  [& [n]]
  (format-timestamp (expiry-later n)))

(defn expiry-now
  "Returns the current instant as the number of **seconds** since the epoch."
  []
  (-> (t/now) c/to-long (quot 1000)))

(defn formatted-expiry-now []
  (format-timestamp (expiry-now)))
