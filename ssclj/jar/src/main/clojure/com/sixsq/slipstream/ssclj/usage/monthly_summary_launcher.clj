(ns com.sixsq.slipstream.ssclj.usage.monthly-summary-launcher
  (:require
    [clojure.tools.cli                                  :as cli]
    [clj-time.core                                      :as t]
    [com.sixsq.slipstream.ssclj.usage.record-keeper     :as rc]
    [com.sixsq.slipstream.ssclj.usage.summary           :as s]
    [com.sixsq.slipstream.ssclj.usage.utils             :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils  :as cu])
  (:gen-class))

(def cli-options
  [["-m" "--month MONTH" "Month to summarize, yyyy-mm, e.g 2015-04"
    :parse-fn #(str % "-01T00:00:00.0Z")
    :validate [cu/valid-timestamp? "Must be a valid month, e.g 2015-04"]]])

(defn month-or-last-month
  [options]
  (or (:month options)
      (let [current-month (->> (t/now)
                               ((juxt t/year t/month))
                               (apply t/date-midnight))]
        (-> current-month
            (t/minus (t/months 1))
            u/to-ISO-8601))))

(defn do-summarize!
  [[start end]]
  (rc/-init)
  (s/summarize-and-store! start end [:cloud])
  (str "Monthly usage summary done for " (u/disp-interval start end)))

(defn -main
  [& args]
  (let [{:keys [options]} (cli/parse-opts args cli-options)
        start (month-or-last-month options)
        end   (-> start
                  u/to-time
                  u/inc-month
                  u/to-ISO-8601)]
    (do-summarize! [start end])))
