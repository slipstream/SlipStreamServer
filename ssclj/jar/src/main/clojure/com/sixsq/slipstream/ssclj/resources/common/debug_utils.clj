(ns com.sixsq.slipstream.ssclj.resources.common.debug-utils)

(defn show 
  [x] 
  (print ":-> ") (prn (class x) x) 
  x)

(defmacro e-> 
 [& body]
 `(-> ~@(interleave body (repeat show))))

(defmacro e->> 
 [& body]
 `(->> ~@(interleave body (repeat show))))