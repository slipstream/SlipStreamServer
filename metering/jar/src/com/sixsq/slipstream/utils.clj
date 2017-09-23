(ns com.sixsq.slipstream.utils)

(defn str->int
  "Converts the argument to a long value if it is a string and contains only
   digits. It returns the value unmodified if it isn't a string or doesn't
   match the correct pattern."
  [s]
  (if (and (string? s) (re-matches #"^\d+$" s))
    (read-string s)
    s))

(defn unwrap
  "Provides a transducer that will apply the reduction function to each
   element of a collection, effectively unwrapping the collection in the
   context of the transducer. If the input is not a collection, the transducer
   will be applied to the value. If the input is an empty collection, then this
   is a no-op."
  []
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result coll]
       (if (not (coll? coll))
         (reduced )
         (rf result coll)
         (loop [result result, coll coll]
           (if (empty? coll)
             result
             (let [resp (rf result (first coll))]
               (if (reduced? resp)
                 resp
                 (recur resp (rest coll)))))))))))
