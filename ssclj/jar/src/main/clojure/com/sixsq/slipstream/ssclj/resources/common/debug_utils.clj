(ns com.sixsq.slipstream.ssclj.resources.common.debug-utils  
  (:require [clojure.pprint :refer [pp pprint]]))
(defn show

  [x]
  (println "SHOW----")
  (println ":-> " (class x))
  (pprint x) 
  (println "--------")
  x)

(defmacro e-> 
 [& body]
 `(-> ~@(interleave body (repeat show))))

(defmacro e->> 
 [& body]
 `(->> ~@(interleave body (repeat show))))