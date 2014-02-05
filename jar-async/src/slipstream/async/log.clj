(ns slipstream.async.log
  (:import [com.sixsq.slipstream.util Logger]))

(defn log-info
  [& msgs]
  (Logger/info (apply str msgs)))

(defn log-error
  [& msgs]
  (Logger/severe (apply str msgs)))
