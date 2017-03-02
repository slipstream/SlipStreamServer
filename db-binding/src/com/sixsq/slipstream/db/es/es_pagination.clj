(ns com.sixsq.slipstream.db.es.es-pagination)

(def ^:const max-result-window 200000)

(def ^:const max-return-size 10000)

(defn- throw-if-size-too-big
  [size]
  (when (> size max-return-size)
    (throw (IllegalArgumentException.
             (str "Size " size " too big. (limit is " max-return-size ")")))))

(defn from-size
  "Returns [from size] from cimi first and last."
  [{:keys [cimi-params]}]
  (let [first (or (:first cimi-params) 1)                   ;; catches also explicit nil values
        last (:last cimi-params)
        from (dec first)
        size (if last (inc (- last first)) max-return-size)]
    (throw-if-size-too-big size)
    (if (some neg? [from size])
      [0 0]
      [from size])))
