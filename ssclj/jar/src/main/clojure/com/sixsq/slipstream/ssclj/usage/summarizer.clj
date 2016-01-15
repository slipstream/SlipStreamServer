(ns com.sixsq.slipstream.ssclj.usage.summarizer
  (:require
    [clojure.set :as set]
    [superstring.core :as string]
    [clojure.tools.cli :as cli]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.usage.summary :as s]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc])
  (:gen-class))

(defn- exception-from-errors
  [errors]
  (->> errors
       (string/join "\n")
       (apply str)
       (IllegalArgumentException.)))

(defn- date-at-monday
  [dt]
  (time/minus dt (time/days (dec (time/day-of-week dt)))))

(defn- now-at-frequency
  "Returns the beginning of the current day, week or month"
  [frequency]
  (let [start-day (time/today-at 0 0)]
    (case frequency
      :daily    start-day
      :weekly   (date-at-monday start-day)
      :monthly  (time/date-time (time/year start-day) (time/month start-day)))))

(defn- previous-at-frequency
  [frequency]
  (-> (now-at-frequency frequency)
      (u/dec-by-frequency frequency)
      u/to-ISO-8601))

(defn- throw-when-errors
  [errors]
  (when errors (throw (exception-from-errors errors))))

(defn- valid-frequency?
  [frequency]
  (-> frequency
      #{:daily :weekly :monthly}
      boolean))

(defn- split-trim
  [s]
  (map string/trim (string/split s #",")))

(defn- split-trim-keywordize
  [s]
  (->>  (split-trim s)
        (map keyword)))

(def cli-options  
  [["-d" "--date DATE" "Date to summarize, yyyy-mm-dd, e.g 2015-04-16"
    :parse-fn #(str % "T00:00:00.000Z")
    :validate [cu/valid-timestamp? "Must be a valid date, e.g 2015-01-15"]]
   ["-f" "--frequency FREQUENCY" "Frequency can be daily, weekly or monthly"
    :parse-fn keyword
    :validate [valid-frequency? "Must be daily, weekly or monthly"]]
   ["-e" "--except user1, user2" "Users to exclude from computation, e.g sixsq_dev, test"
    :parse-fn split-trim
    :default []]
   ["-g" "--grouped-by dimensions" "e.g [:user :cloud], or [:cloud]"
    :default [:user :cloud]
    :parse-fn split-trim-keywordize]
   ["-n" "--number to compute" "Number of periods to compute (backward!)"
    :parse-fn read-string
    :default 1]])

(defn- date-or-previous
  [options frequency]
  (or (:date options)
      (previous-at-frequency frequency)))

(defn- move-ts
  [ts frequency move-fn]
  (-> ts
      u/to-time
      (move-fn frequency)
      u/to-ISO-8601))

(defn- next-ts
  [frequency ts]
  (move-ts ts frequency u/inc-by-frequency))

(defn- previous-ts
  [frequency ts]
  (move-ts ts frequency u/dec-by-frequency))

(defn- check-required
  [required-options options]
  (let [missing-required-options (set/difference required-options (-> options keys set))]
    (when (not-empty missing-required-options)
      (throw (IllegalArgumentException.
               (str "Missing required options: " (mapv name missing-required-options)))))))

(defn parse-args
  [args]
  (let [{:keys [options errors]}  (cli/parse-opts args cli-options)
        _                         (throw-when-errors errors)
        _                         (check-required #{:frequency} options)
        frequency                 (:frequency options)
        start                     (date-or-previous options frequency)
        except-users              (:except options)
        grouped-by                (:grouped-by options)
        n                         (:number options)]
    [start frequency except-users grouped-by n]))

(defn backward-periods
  [start n frequency]
  (->> start
       (next-ts frequency)
       (iterate (partial previous-ts frequency))
       (take (inc n))
       reverse
       (partition 2 1)))

(defn -main
  "See tests for examples on how to call from clojure REPL"
  [& args]
  (let [[start frequency except-users grouped-by n] (parse-args args)]
    (rc/-init)
    (doseq [[start end] (backward-periods start n frequency)]
      (println "summarizing " start " -> " end)
      (s/summarize-and-store! start end frequency grouped-by except-users))))
