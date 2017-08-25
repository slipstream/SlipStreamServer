(ns com.sixsq.slipstream.db.es.pagination
  (:import (org.elasticsearch.action.search SearchRequestBuilder)))

(def ^:const max-size 10000)

(defn es-paging-params
  "Returns [from size] based on the values 'first' and 'last'. If the value of
   'last' is zero, then a zero size is always returned. If the size exceeds the
   maximum, then an exception is thrown."
  [first last]
  (let [first (max 1 (or first 1))
        from (dec first)
        size (cond
               (nil? last) max-size
               (zero? last) 0
               (>= last first) (inc (- last first))
               :else 0)]
    (if (<= size max-size)
      [from size]
      (throw (IllegalArgumentException.
               (str "size " size " too large; limit is " max-size))))))

(defn add-paging
  "Adds the paging parameters 'from' and 'size' to the request builder based
   on the 'first' and 'last' CIMI parameter values. Note that a 'last' value of
   zero is a special case that always results in a size of zero."
  [^SearchRequestBuilder request-builder {:keys [first last] :as cimi-params}]
  (let [[from size] (es-paging-params first last)]
    (.. request-builder
        (setFrom from)
        (setSize size))))
