(ns com.sixsq.slipstream.ssclj.usage.utils
 (:require 
  [clojure.tools.logging :as log]
  [clj-time.core :as time]
  [clj-time.format :as time-fmt]))

(defn to-time
  "Tries to parse the given string as a DateTime value.  Returns the DateTime
   instance on success and nil on failure."
  [s]
  (time-fmt/parse (:date-time time-fmt/formatters) s))

(defn- log-and-throw 
  [msg]
  (log/error msg)
  (throw (IllegalArgumentException. msg)))

(defn check   
  [pred coll msg-error]
  (if-not (pred coll)
    (log-and-throw msg-error)
    coll))

(defn start-before-end?   
  [[t1 t2]]
  (or 
    (nil? t2)
    (time/before? t1 t2))) 

(defn max-time   
  [t1 t2]
  (if (time/after? t1 t2)
    t1
    t2))

(defn min-time   
  [t1 t2]
  (if (time/before? t1 t2)
    t1
    t2))
