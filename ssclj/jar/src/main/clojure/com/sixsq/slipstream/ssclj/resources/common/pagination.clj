(ns com.sixsq.slipstream.ssclj.resources.common.pagination)

(defn- cut-last
  [col first last]
  (if last
    (if first
      (take (inc (- last first)) col)
      (take last col))
    col))

(defn- cut-first
  [col first]
  (if first
    (drop (dec first) col)
    col))

(defn paginate
  [first last col]
  (-> col
      (cut-first first)
      (cut-last first last)))
