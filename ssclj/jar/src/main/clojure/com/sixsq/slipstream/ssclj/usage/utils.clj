(ns com.sixsq.slipstream.ssclj.usage.utils
  (:require
    [clojure.tools.logging :as log]
    [clojure.data.json :as json]
    [clj-time.core :as time]
    [clj-time.format :as time-fmt]))

(defn timestamp
  [& args]
  (time-fmt/unparse
    (:date-time time-fmt/formatters)
    (apply time/date-time args)))

(defn period-fn
  [frequency]
  (frequency {:daily time/days :weekly time/weeks :monthly time/months
              :hourly time/hours :minutely time/minutes}))

(defn dec-by-frequency
  [dt frequency]
  (time/minus dt ((period-fn frequency) 1)))

(defn inc-by-frequency
  [dt frequency]
  (time/plus dt ((period-fn frequency) 1)))

(defn inc-minutes
  [ts minutes]
  (time/plus ts (time/minutes minutes)))

(defn to-time
  "Tries to parse the given string as a DateTime value.  Returns the DateTime
   instance on success and nil on failure."
  [s]
  (time-fmt/parse (:date-time time-fmt/formatters) s))

(defn to-ISO-8601
  [dt]
  (time-fmt/unparse (:date-time time-fmt/formatters) dt))

(defn now-to-ISO-8601
  []
  (to-ISO-8601 (time/now)))

(defn timestamp-next-frequency
  "Some examples:
  (timestamp-next-frequency :daily 2005 11 23)   -> 2005-11-24T00:00:00.000Z
  (timestamp-next-frequency :weekly 2005 11 23)  -> 2005-11-30T00:00:00.000Z
  (timestamp-next-frequency :monthly 2005 11 23) -> 2005-12-23T00:00:00.000Z"
  [frequency & args]
  (-> (apply time/date-time args)
      (inc-by-frequency frequency)
      to-ISO-8601))

(defn timestamps-from
  "Returns an infinite lazy sequence of timestamps starting from given datetime (in args)
  separated by given period (see period-fn for possible values)"
  [period & args]
  (map to-ISO-8601
    (iterate #(inc-by-frequency % period)
             (apply time/date-time args))))

(defn to-interval
  [start end]
  (time/interval (to-time start) (to-time end)))

(defn disp-interval
  [start end]
  (str "[" start " / " end "]"))

(defn- log-and-throw
  [msg-error]
  (log/error msg-error)
  (throw (IllegalArgumentException. msg-error)))

(defn check
  [pred e msg-error]
  (if-not (pred e)
    (log-and-throw msg-error)
    e))

(defn start-before-end?
  [[t1 t2]]
  (time/before? (to-time t1) (to-time t2)))

(defn check-order
  [[start end]]
  (check start-before-end? [start end] (str (disp-interval start end) ": invalid period (respect order!)")))

(defn serialize
  [m]
  (with-out-str
    (json/pprint m :key-fn name)))

(defn deserialize
  [s]
  (json/read-str s :key-fn keyword))

(defn max-time
  [t1 t2]
  (if (time/after? (to-time t1) (to-time t2))
    t1
    t2))

(defn min-time
  [t1 t2]
  (cond
    (nil? t1) t2
    (nil? t2) t1
    (time/before? (to-time t1) (to-time t2)) t1
    :else t2))


