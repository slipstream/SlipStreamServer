(require '[clojure.core.async :as async :refer :all])

(defn launch
  [run user]
(let [c (chan)
      begin (System/currentTimeMillis)]
  (alts!! [c (timeout 100)])
