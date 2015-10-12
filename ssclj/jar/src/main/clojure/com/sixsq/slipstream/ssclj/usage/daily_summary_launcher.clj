(ns com.sixsq.slipstream.ssclj.usage.daily-summary-launcher
  (:require
    [clojure.tools.cli :as cli]
    [clj-time.core :as t]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.summary :as s]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.usage.utils :as u])
  (:gen-class))

(def cli-options  
  [["-d" "--date DATE" "Date to summarize, yyyy-mm-dd, e.g 2015-04-16"  
    :parse-fn #(str % "T00:00:00.0Z")
    :validate [cu/valid-timestamp? "Must be a valid date, e.g 2015-01-15"]]])

(defn date-or-yesterday   
  [options]
  (or (:date options)
      (-> (t/today-at 0 0)
          (t/minus (t/days 1))
          u/to-ISO-8601)))

(defn do-summarize!
  [[start end]]
  (rc/-init)
  (s/summarize-and-store! start end [:user :cloud])
  (str "Daily usage summary done for " (u/disp-interval start end)))

(defn -main
  "See tests for examples on how to call from clojure REPL"
  [& args]    
  (let [{:keys [options]} (cli/parse-opts args cli-options)
        start (date-or-yesterday options)        
        end   (-> start
                  u/to-time
                  u/inc-day
                  u/to-ISO-8601)]
    (do-summarize! [start end])))