(ns com.sixsq.slipstream.ssclj.usage.launcher
  (:require 
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]    
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.summary :as s]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    )
  (:gen-class))

(defn fill-up-to-timestamp
  [ts]
  (let [timestamp-filler "XXXX-01-01T00:00:00.0Z"]
    (cond 
      (< (count ts) 4) 
        (throw (IllegalArgumentException. (str "Given timestamp '" ts "' is too short. (Minimum yyyy)")))
      (< (count timestamp-filler) (count ts)) 
        (throw (IllegalArgumentException. (str "Given timestamp '" ts "' is too long. (Maximum 2015-01-15T00:00:00.0Z)")))
      :else
        (str ts (subs timestamp-filler (count ts))))))

(def cli-options  
  [["-s" "--start START_TIMESTAMP" "Start timestamp"  
    :parse-fn fill-up-to-timestamp
    :validate [cu/valid-timestamp? "Must be a valid timestamp (e.g 2015, 2015-01, ... 2015-01-15T00:00:00.0Z)"]]
        
   ["-e" "--end START_TIMESTAMP" "End timestamp"  
    :parse-fn fill-up-to-timestamp
    :validate [cu/valid-timestamp? "Must be a valid timestamp (e.g 2015, 2015-01, ... 2015-01-16T00:00:00.0Z)"]]
   ["-h" "--help"]])

(defn usage 
  [options-summary]
  (->> ["Triggers a usage summary computation for the given period."
        ""
        "Usage: launcher -s START_TIMESTAMP -e END_TIMESTAMP"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn error-msg 
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn wrong-nb-args-msg 
  [errors]
  (str "Wrong number of arguments provided:\n\n"
       (string/join \newline errors)))

(defn exit 
  [status msg]
  (println msg)
  (System/exit status))

(defn check-order   
  [options]
  (let [[start end] [(:start options) (:end options)]]    
    (if (u/start-before-end? [start end])
      [:success [start end]]
      [:error   (str "Invalid period " (u/disp-interval start end))])))

(defn analyze-args   
  [args]  
  (let [{:keys [options arguments errors summary] :as all} (parse-opts args cli-options)]      
    ; (clojure.pprint/pprint all)  ;; TODO
    (cond
      (:help options) [:help    (usage summary)]      
      errors          [:error   (error-msg errors)]

      :else           (check-order options))))

(defn- do-summarize   
  [[start end]]
  (rc/-init)
  (s/summarize-and-store start end)
  (str "Summary done for " (u/disp-interval start end)))

(defn -main 
  [& args]  
  (let [[code data] (analyze-args args)]
    (case code
      :help           (exit 0 data)      
      :error          (exit 1 data)
      :success        (exit 0 (do-summarize data)))))
      
