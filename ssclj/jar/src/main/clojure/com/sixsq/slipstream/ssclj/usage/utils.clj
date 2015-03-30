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
  [msg-error]
  (log/error msg-error)
  (throw (IllegalArgumentException. msg-error)))

(defn check  
  [pred e msg-error]  
  (if-not (pred e)
    (log-and-throw msg-error)
    e))

(defn start-before-end?   
  ;; TODO t1 and t2 are strings
  [[t1 t2]]  
  (time/before? (to-time t1) (to-time t2)))

(defn start-before-end-or-open?   
  ;; TODO t1 and t2 are times : HOMOGENEÏSE !!!
  [[t1 t2]]
  (or 
    (nil? t2)
    (time/before? t1 t2))) 

(defn max-time   
  [t1 t2]
  (if (time/after? (to-time t1) (to-time t2))
    t1
    t2))

(defn min-time   
  [t1 t2]
  (if (time/before? (to-time t1) (to-time t2))
    t1
    t2))
