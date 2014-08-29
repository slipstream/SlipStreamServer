(ns slipstream.async.log
  (:import [com.sixsq.slipstream.util Logger]))

(defn log-info
  [& msgs]
  (Logger/info (apply str msgs)))

(defn log-warn
  [& msgs]
  (Logger/warning (apply str msgs)))

(defn log-error
  [& msgs]
  (Logger/severe (apply str msgs)))

(defn log-debug
  [& msgs]
  (Logger/debug (apply str msgs)))
