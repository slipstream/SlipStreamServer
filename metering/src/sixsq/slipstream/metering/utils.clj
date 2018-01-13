(ns sixsq.slipstream.metering.utils
  (:import (java.util UUID)))

(defn str->int [s]
  (if (and (string? s) (re-matches #"^\d+$" s))
    (read-string s)
    s))

(defn random-uuid
  []
  (str (UUID/randomUUID)))
