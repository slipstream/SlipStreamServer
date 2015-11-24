(ns com.sixsq.slipstream.ssclj.usage.monthly-summary-launcher
  (:require
    [clojure.tools.cli                                  :as cli]
    [clj-time.core                                      :as t]
    [clojure.string                                     :as cs]
    [com.sixsq.slipstream.ssclj.usage.utils             :as u]
    [com.sixsq.slipstream.ssclj.resources.common.utils  :as cu]
    [com.sixsq.slipstream.ssclj.usage.summary-launcher  :as sl])
  (:gen-class))

(def cli-options
  [["-m" "--month MONTH" "Month to summarize, yyyy-mm, e.g 2015-04"
    :parse-fn #(str % "-01T00:00:00.0Z")
    :validate [cu/valid-timestamp? "Must be a valid month, e.g 2015-04"]]
   ["-e" "--except user1, user2" "Users to exclude from computation, e.g sixsq_dev, test"]])

(defn- month-or-last-month
  [options]
  (or (:month options)
      (let [current-month (->> (t/now)
                               ((juxt t/year t/month))
                               (apply t/date-midnight))]
        (-> current-month
            (t/minus (t/months 1))
            u/to-ISO-8601))))

(defn- except-users
  [options]
  (if (:except options)
    (->> (-> options
             :except
             (cs/split #","))
         (map cs/trim))
    []))

(defn -main
  [& args]
  (let [{:keys [options errors]}  (cli/parse-opts args cli-options)
        start                     (month-or-last-month options)
        end                       (-> start
                                      u/to-time
                                      (u/inc-by-frequency :monthly)
                                      u/to-ISO-8601)
        except-users              (except-users options)]

    (sl/throw-when-errors errors)
    (sl/do-summarize! [start end] :monthly [:cloud] except-users)))
