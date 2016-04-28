(ns com.sixsq.slipstream.ssclj.es.es-pagination)

(def ^:const max-result-window 10000)

(defn- get-in-no-nil
  [m ks default-value]
  (if-let [v (get-in m ks)]
    v
    default-value))

(defn- first-value
  [cimi-params]
  (get-in-no-nil cimi-params [:cimi-params :first] 1))

(defn- last-value
  [cimi-params]
  (get-in cimi-params [:cimi-params :last]))

(defn- throw-if-size-too-big
  [size]
  (when (> size max-result-window)
    (throw (IllegalArgumentException.
             (str "Size " size " too big. (limit is " max-result-window ")")))))

(defn from-size
  "Returns [from size] from cimi first and last."
  [cimi-params]
  (let [first (first-value cimi-params)
        last  (last-value cimi-params)
        from  (dec first)
        size  (if last (inc (- last first)) max-result-window)]
    (throw-if-size-too-big size)
    (if (some neg? [from size])
      [0 0]
      [from size])))
