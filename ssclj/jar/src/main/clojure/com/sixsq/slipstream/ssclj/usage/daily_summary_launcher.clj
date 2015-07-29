(ns com.sixsq.slipstream.ssclj.usage.daily-summary-launcher
	(:require 
    [clojure.tools.cli                                  :as cli]
    [clj-time.core                                      :as t]
    [com.sixsq.slipstream.ssclj.usage.utils             :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils  :as cu]
    [com.sixsq.slipstream.ssclj.usage.launcher          :as l])
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

(defn -main 
  [& args]    
  (let [{:keys [options arguments errors summary] :as all} (cli/parse-opts args cli-options)
        start (date-or-yesterday options)        
        end   (-> start
                  u/to-time
                  (t/plus (t/days 1))
                  u/to-ISO-8601)]
    (l/do-summarize [start end])))