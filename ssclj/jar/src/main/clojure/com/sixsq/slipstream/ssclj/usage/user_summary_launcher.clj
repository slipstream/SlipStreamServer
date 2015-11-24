(ns com.sixsq.slipstream.ssclj.usage.user-summary-launcher
  (:require
    [clojure.tools.cli :as cli]
    [clj-time.core :as t]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.usage.summary-launcher :as sl])
  (:gen-class))

(defn valid-frequency?
  [frequency]
  (-> frequency
      #{:daily :weekly :monthly}
      boolean))

(def cli-options  
  [["-d" "--date DATE" "Date to summarize, yyyy-mm-dd, e.g 2015-04-16"
    :parse-fn #(str % "T00:00:00.0Z")
    :validate [cu/valid-timestamp? "Must be a valid date, e.g 2015-01-15"]]
   ["-f" "--frequency FREQUENCY" "Frequency can be daily, weekly or monthly"
    :parse-fn keyword
    :required "frequency (-f or --frequency) is required"
    :validate [valid-frequency? "Must be daily, weekly or monthly"]]])

(defn- date-or-yesterday
  [options]
  (or (:date options)
      (-> (t/today-at 0 0)
          (t/minus (t/days 1))
          u/to-ISO-8601)))

(defn- next-day
  [ts]
  (-> ts
      u/to-time
      (u/inc-by-frequency :daily)
      u/to-ISO-8601))

(defn -main
  "See tests for examples on how to call from clojure REPL"
  [& args]    
  (let [{:keys [options errors]}  (cli/parse-opts args cli-options)
        frequency                 (:frequency options)
        start                     (date-or-yesterday options)
        end                       (next-day start)]

    (sl/throw-when-errors errors)
    (sl/do-summarize! [start end] frequency [:user :cloud] [])))