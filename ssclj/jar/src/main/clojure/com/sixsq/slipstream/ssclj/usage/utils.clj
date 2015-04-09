(ns com.sixsq.slipstream.ssclj.usage.utils
 (:require 
  [clojure.tools.logging :as log]
  [clojure.data.json :as json]
  [clj-time.core :as time]
  [clj-time.format :as time-fmt]))

(defn to-time
  "Tries to parse the given string as a DateTime value.  Returns the DateTime
   instance on success and nil on failure."
  [s]
  (time-fmt/parse (:date-time time-fmt/formatters) s))

(defn to-ISO-8601
  [ts]
  (time-fmt/unparse (:date-time time-fmt/formatters) ts))

(defn to-interval   
  [start end]    
  (time/interval (to-time start) (to-time end)))

(defn serialize
  [m]
  (with-out-str
   (json/pprint m :key-fn name)))

(defn deserialize
  [s]
  (json/read-str s :key-fn keyword))

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
  [[t1 t2]]  
  (time/before? (to-time t1) (to-time t2)))

(defn max-time   
  [t1 t2]
  (if (time/after? (to-time t1) (to-time t2))
    t1
    t2))

(defn min-time   
  [t1 t2]
  (cond
    (nil? t1) t2
    (nil? t2) t1
    (time/before? (to-time t1) (to-time t2)) t1
    :else t2))

(defn clojurify   
  [exp] 
  (cond
    (instance? java.util.Map exp) (into {} (for [[k v] exp] [(keyword k) v]))
    (instance? java.util.List exp) (into [] exp)
    :else exp))

(defn walk-clojurify
  [java-map]
  (clojure.walk/prewalk clojurify java-map))

