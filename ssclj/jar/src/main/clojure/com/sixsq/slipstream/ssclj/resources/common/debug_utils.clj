(ns com.sixsq.slipstream.ssclj.resources.common.debug-utils)

(defn show 
  [x] 
  (println ":-> " (class x))
  (clojure.pprint/pprint x) 
  x)

(defmacro e-> 
 [& body]
 `(-> ~@(interleave body (repeat show))))

(defmacro e->> 
 [& body]
 `(->> ~@(interleave body (repeat show))))