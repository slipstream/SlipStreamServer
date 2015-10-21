(ns com.sixsq.slipstream.ssclj.usage.summary-launcher
  (:require
    [clojure.string :as cs]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.summary :as s])
  (:gen-class))

(defn- exception-from-errors
  [errors]
  (->> errors
       (cs/join "\n")
       (apply str)
       (IllegalArgumentException.)))

(defn throw-when-errors
  [errors]
  (when errors (throw (exception-from-errors errors))))

(defn do-summarize!
  [[start end] columns except-users]
  (rc/-init)
  (s/summarize-and-store! start end columns except-users))
