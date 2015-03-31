(ns com.sixsq.slipstream.ssclj.usage.summary-volume-test
  (:require    
    [com.sixsq.slipstream.ssclj.usage.summary :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [clojure.test :refer :all]
    [clojure.tools.logging :as log]
    [korma.core :refer :all]
    [clj-time.format :as f]
    [clj-time.core :as t]))

(defn delete-all [f]
  (rc/-init)
  (defentity usage-records)
  (defentity usage-summaries)
  (delete usage-records)
  (delete usage-summaries)
  (log/debug "All usage-records deleted")
  (log/debug "usage records " (select usage-records))
  (log/debug "usage summaries " (select usage-summaries))
  (f))
(use-fixtures :each delete-all)

(def event-joe-start-template
  { 
    :user                "joe"
    :cloud               "exoscale-ch-gva"    
    :metrics [{   :name  "nb-cpu"
                  :value 2 }
              { :name  "RAM-GB"
                  :value 8 }
              { :name  "disk-GB"
                  :value 100.5 }]})

(def event-end-template
  { :cloud_vm_instanceid "exoscale-ch-gva:abcd" })

(defn fill-joe 
  [nb-days]
  (doseq [day (range nb-days)]
    (let [start-day (t/plus (t/date-time 2014) (t/days day))
          day-9h    (t/plus start-day (t/hours 9))
          day-14h   (t/plus start-day (t/hours 14))
          vm-joe-id (str "exo" day)
          event-joe-start (assoc event-joe-start-template 
                              :start_timestamp day-9h 
                              :cloud_vm_instanceid vm-joe-id)
          event-joe-end {:end_timestamp day-14h
                         :cloud_vm_instanceid vm-joe-id}
      ]
      (rc/-insertStart event-joe-start)
      (rc/-insertEnd event-joe-end))))

(defn summarize-joe-weekly 
  [nb-weeks]
  (doseq [i (range nb-weeks)]
    (let [start-week (t/plus (t/date-time 2014) (t/weeks i))
          end-week (t/plus start-week (t/weeks 1))]
      (summarize-and-store (u/to-ISO-8601 start-week) (u/to-ISO-8601 end-week)))))      

(defn check-summaries
  []  
  (doseq [summary (select usage-summaries)]
    (is (= "exoscale-ch-gva"  (:cloud summary)))
    (is (= "joe"              (:user summary)))
    (is (= {:disk-GB {:unit_minutes 211050.0}, 
            :RAM-GB {:unit_minutes 16800.0}, 
            :nb-cpu {:unit_minutes 4200.0}}) (u/deserialize (:usage summary)))    
    ))

(deftest test-with-records-full-year
  (let [nb-weeks 5]
    (fill-joe (* nb-weeks 7))
    (summarize-joe-weekly nb-weeks)
    (check-summaries)))