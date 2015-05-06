(ns com.sixsq.slipstream.ssclj.usage.summary-volume-test
  (:refer-clojure :exclude [update])
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
  (defentity usage_records)
  (defentity usage_summaries)
  (delete usage_records)
  (delete usage_summaries)
  (log/debug "All usage_records deleted")
  (log/debug "usage records " (select usage_records))
  (log/debug "usage summaries " (select usage_summaries))
  (f))
(use-fixtures :each delete-all)

(def jack-exoscale
  { 
    :user                "jack"
    :cloud               "exoscale-ch-gva"    
    :metrics [{   :name  "small"
                  :value 2.0 }
              { :name  "big"
                  :value 4.0 }]})

(def joe-exoscale
  { 
    :user                "joe"
    :cloud               "exoscale-ch-gva"    
    :metrics [{   :name  "nb-cpu"
                  :value 2.0 }
              { :name  "RAM-GB"
                  :value 8.0 }
              { :name  "disk-GB"
                  :value 100.5 }]})

(def joe-aws
  { 
    :user                "joe"
    :cloud               "aws"    
    :metrics [{   :name  "nb-cpu"
                  :value 4.0 }
              { :name  "RAM-GB"
                  :value 16.0 }
              { :name  "disk-GB"
                  :value 510.0 }]})

(def event-end-template
  { :cloud_vm_instanceid "exoscale-ch-gva:abcd" })

(defn fill-joe 
  [nb-days]
  (doseq [day (range nb-days)]
    (let [start-day       (t/plus (t/date-time 2014) (t/days day))
          day-9h          (t/plus start-day (t/hours 9))
          day-11h         (t/plus start-day (t/hours 11))
          day-13h         (t/plus start-day (t/hours 13))
          day-14h         (t/plus start-day (t/hours 14))
          vm-joe-exo-id   (str "exo" day)
          vm-joe-aws-id   (str "aws" day)

          joe-exo-start (assoc joe-exoscale 
                              :start_timestamp (u/to-ISO-8601 day-9h)
                              :cloud_vm_instanceid vm-joe-exo-id)
          joe-exo-end {:end_timestamp (u/to-ISO-8601 day-14h)
                         :cloud_vm_instanceid vm-joe-exo-id}

          joe-aws-start (assoc joe-aws 
                              :start_timestamp (u/to-ISO-8601 day-11h)
                              :cloud_vm_instanceid vm-joe-aws-id)
          joe-aws-end {:end_timestamp (u/to-ISO-8601 day-13h)
                         :cloud_vm_instanceid vm-joe-aws-id}

      ]      
      (rc/-insertStart joe-exo-start)
      (rc/-insertStart joe-aws-start)

      (rc/-insertEnd joe-exo-end)
      (rc/-insertEnd joe-aws-end))))

(defn summarize-joe-weekly 
  [nb-weeks]
  (doseq [i (range nb-weeks)]
    (let [start-week (t/plus (t/date-time 2014) (t/weeks i))
          end-week (t/plus start-week (t/weeks 1))]
      (summarize-and-store (u/to-ISO-8601 start-week) (u/to-ISO-8601 end-week)))))      

(defn check-summaries
  []  
  (doseq [summary (select usage_summaries)]    
    (if (= "exoscale-ch-gva"  (:cloud summary))
      (do
        (is (= "joe"              (:user summary)))
        (is (= {:disk-GB {:unit_minutes 211050.0}, 
                :RAM-GB {:unit_minutes 16800.0}, 
                :nb-cpu {:unit_minutes 4200.0}} (u/deserialize (:usage summary)))))
      (do
        (is (= "aws"              (:cloud summary)))
        (is (= "joe"              (:user summary)))
        (is (= {:disk-GB {:unit_minutes 428400.0}, 
                :RAM-GB {:unit_minutes 13440.0}, 
                :nb-cpu {:unit_minutes 3360.0}} (u/deserialize (:usage summary)))))
    )))

(deftest check-precision-small-interval
  (let [start-day         (t/date-time 2014)
        end-day           (t/plus (t/date-time 2014) (t/days 1))

        fifty-seconds-after (t/plus start-day (t/seconds 50))

        joe-exo-start (assoc joe-exoscale 
                              :start_timestamp (u/to-ISO-8601 start-day)
                              :cloud_vm_instanceid "vm-joe-exo-id")
        joe-exo-end { :end_timestamp (u/to-ISO-8601 fifty-seconds-after)
                      :cloud_vm_instanceid "vm-joe-exo-id"}]      

      (rc/-insertStart joe-exo-start)
      (rc/-insertEnd joe-exo-end)
      (summarize-and-store (u/to-ISO-8601 start-day) (u/to-ISO-8601 end-day))

      (let [summary (-> (select usage_summaries)
                        first
                        :usage  
                        u/deserialize)]
        (is (= 83.75 (get-in summary [:disk-GB :unit_minutes])))
        (is (< 6.666 (get-in summary [:RAM-GB :unit_minutes]) 6.667))
        (is (< 1.666 (get-in summary [:nb-cpu :unit_minutes]) 1.667)))))


(deftest test-with-records-full-year
  (let [nb-weeks 5]
    (fill-joe (* nb-weeks 7))
    (summarize-joe-weekly nb-weeks)
    (check-summaries)))

(deftest multiple-users-same-cloud
  (let [start-day         (t/date-time 2014)
        end-day           (t/plus (t/date-time 2014) (t/days 1))

        joe-exo-start (assoc joe-exoscale   
                              :start_timestamp (u/to-ISO-8601 start-day)
                              :cloud_vm_instanceid "vm-joe-exo-id")
        joe-exo-end { :end_timestamp (u/to-ISO-8601 (t/plus start-day (t/days 1)))
                      :cloud_vm_instanceid "vm-joe-exo-id"}      

        jack-exo-start (assoc jack-exoscale 
                              :start_timestamp (u/to-ISO-8601 start-day)
                              :cloud_vm_instanceid "vm-jack-exo-id")
        jack-exo-end {:end_timestamp (u/to-ISO-8601 (t/plus start-day (t/days 1)))
                      :cloud_vm_instanceid "vm-jack-exo-id"}]      

      (rc/-insertStart joe-exo-start)
      (rc/-insertStart jack-exo-start)
      (rc/-insertEnd joe-exo-end)
      (rc/-insertEnd jack-exo-end)

      (summarize-and-store (u/to-ISO-8601 start-day) (u/to-ISO-8601 end-day))
      
      (let [summaries (select usage_summaries)]
        (is (= [
          {:disk-GB {:unit_minutes 144720.0}, :RAM-GB {:unit_minutes 11520.0}, :nb-cpu {:unit_minutes 2880.0}}] 
          (map (comp u/deserialize :usage) (filter #(= "joe" (:user %)) summaries))))
        (is (= [
          {:big {:unit_minutes 5760.0}, :small {:unit_minutes 2880.0}}]
          (map (comp u/deserialize :usage) (filter #(= "jack" (:user %)) summaries)))))))

(deftest summarize-open-record
  (let [start-day         (t/date-time 2014)
        end-day           (t/plus (t/date-time 2014) (t/days 1))
        joe-exo-start (assoc joe-exoscale   
                              :start_timestamp (u/to-ISO-8601 start-day)
                              :cloud_vm_instanceid "vm-joe-exo-id")]

      (rc/-insertStart joe-exo-start)

      (summarize-and-store (u/to-ISO-8601 (t/minus start-day (t/days 1))) (u/to-ISO-8601 end-day))
      
      (let [summaries (select usage_summaries)]
        (is (= [{:disk-GB {:unit_minutes 144720.0}, :RAM-GB {:unit_minutes 11520.0}, :nb-cpu {:unit_minutes 2880.0}}] 
          (map (comp u/deserialize :usage) (select usage_summaries)))))))



