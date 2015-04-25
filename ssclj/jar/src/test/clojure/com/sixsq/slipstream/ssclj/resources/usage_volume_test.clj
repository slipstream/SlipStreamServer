(ns com.sixsq.slipstream.ssclj.resources.usage-volume-test
  (:require
    [clojure.test                                               :refer :all]
    [clj-time.core                                              :as time]    
    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils                     :as u]
    [com.sixsq.slipstream.ssclj.resources.usage-test            :as ut]))

(use-fixtures :each ut/reset-summaries)

(def users  (map #(str "joe" %) (range)))
(def days   (u/days-after 2015))

(deftest mass-populate-summaries
  (doseq [user (take 1 users) day (take 1 days) cloud ["exo" "aws" "dig"]]    
    (rc/insert-summary! 
      (ut/daily-summary user 
                        cloud 
                        ((juxt time/year time/month time/day) day) 
                        {:ram { :unit_minutes 100.0}})))
  (println )

  (is (= 1 1)))
