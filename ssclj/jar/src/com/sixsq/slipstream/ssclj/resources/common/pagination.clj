(ns com.sixsq.slipstream.ssclj.resources.common.pagination)

(defn- cut-last
  [coll first last]
  (if last
    (if first
      (take (inc (- last first)) coll)
      (take last coll))
    coll))

(defn- cut-first
  [coll first]
  (if first
    (drop (dec first) coll)
    coll))

(defn paginate
  [first last coll]
  (-> coll
      (cut-first first)
      (cut-last first last)))
