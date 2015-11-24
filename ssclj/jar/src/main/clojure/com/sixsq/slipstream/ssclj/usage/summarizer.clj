(ns com.sixsq.slipstream.ssclj.usage.summarizer
  (:require
    [clojure.set :as set]
    [clojure.string :as string]
    [clojure.tools.cli :as cli]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.usage.summary :as s]
    [clj-time.core :as t]
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

(defn- start-at-frequency
  [frequency]
  (let [start-day (t/today-at 0 0)]
    (case frequency
      :daily    start-day
      :weekly   (date-at-monday start-day)
      :monthly  (t/date-time (t/year start-day) (t/month start-day)))))

(defn- previous-at-frequency
  [frequency]
  (-> (start-at-frequency frequency)
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
    :parse-fn split-trim-keywordize]])

(defn- date-or-previous
  [options frequency]
  (or (:date options)
      (previous-at-frequency frequency)))

(defn- next-period
  [ts frequency]
  (-> ts
      u/to-time
      (u/inc-by-frequency frequency)
      u/to-ISO-8601))

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
        end                       (next-period start frequency)
        except-users              (:except options)
        grouped-by                (:grouped-by options)]
    [start end frequency except-users grouped-by]))

(defn -main
  "See tests for examples on how to call from clojure REPL"
  [& args]
  (let [[start end frequency except-users grouped-by] (parse-args args)]
    (rc/-init)
    (s/summarize-and-store! start end frequency grouped-by except-users)))