(ns com.sixsq.slipstream.ssclj.usage.summary-test
  (:require    
    [com.sixsq.slipstream.ssclj.usage.summary :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [clojure.test :refer :all]
    [clojure.tools.logging :as log]
    [korma.core :refer :all]
    [clj-time.format :as f]
    [clj-time.core :as t]))

(defn timestamp
  [& args]
  (f/unparse (:date-time f/formatters) (apply t/date-time args)))  

(def past-1 (timestamp 2015 04 12))
(def past-2 (timestamp 2015 04 13))
(def after-day (timestamp 2015 04 17 3))

(def start-day  (timestamp 2015 04 16))
(def in-day-1  (timestamp 2015 04 16 9 33))
(def in-day-2  (timestamp 2015 04 16 15 10))

(def end-day    (timestamp 2015 04 17))

(def future-1 (timestamp 2015 04 20))
(def future-2 (timestamp 2015 04 22))

(defn delete-all [f]
  (rc/-init)
  (defentity usage-records)
  (defentity usage-summaries)
  (delete usage-records)
  ; (delete usage-summaries)
  (log/debug "All usage-records deleted")
  (log/debug "usage records " (select usage-records))
  (log/debug "usage summaries " (select usage-summaries))
  (f))
(use-fixtures :each delete-all)

(deftest truncate-filters-outside-records
  (let [urs [{:start_timestamp in-day-1 :end_timestamp in-day-2}]]
    (is (= urs (truncate start-day end-day urs )))
    (is (= urs (truncate past-1 future-1 urs)))
    (is (= [] (truncate past-1 past-2 urs))
    (is (= [] (truncate future-1 future-2 urs))
      ))))

(deftest truncate-checks-args
  (truncate past-1 past-2 [])
  (is (thrown? IllegalArgumentException (truncate past-2 past-1 []))))
  
(deftest truncate-move-start
  (let [urs [{:start_timestamp past-1 :end_timestamp in-day-2}]]
    (is (= [{:start_timestamp start-day :end_timestamp in-day-2}]
        (truncate start-day end-day urs)))))

(deftest truncate-move-end
  (let [urs [{:start_timestamp in-day-1 :end_timestamp future-1}]]
    (is (= [{:start_timestamp in-day-1 :end_timestamp end-day}]
        (truncate start-day end-day urs)))))

(deftest test-contribution
  (is (= 11692.8 (contribution {:start_timestamp start-day :end_timestamp end-day :metric_value 8.12}))))

(def record-1  
  { :cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     in-day-1  
    :end_timestamp       in-day-2
    :metric_name        "nb-cpu"
    :metric_value       4 })

(def record-2  
  { :cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     start-day  
    :end_timestamp       in-day-2
    :metric_name         "nb-cpu"
    :metric_value        6 })

(def record-3  
  { :cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     in-day-1  
    :end_timestamp       end-day
    :metric_name         "RAM"
    :metric_value        16 })

(def record-4 
  { :cloud_vm_instanceid "aws:445623"
    :user                "joe"
    :cloud               "aws"
    :start_timestamp     past-1  
    :end_timestamp       future-2
    :metric_name         "Disk"
    :metric_value        100 })

(def record-5 
  { :cloud_vm_instanceid "aws:445623"
    :user                "joe"
    :cloud               "aws"
    :start_timestamp     past-1  
    :end_timestamp       nil
    :metric_name         "Disk"
    :metric_value        100 })

(deftest test-summarize-records
  (is (=
    [{ 
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     start-day   
    :end_timestamp       end-day
    :usage 
      {
        "nb-cpu"
          {
            :cloud_vm_instanceid      "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"          
            ;; 337 minutes between in-day-1 and in-day-1, 910 minutes from start day to in-
            ; :aggregated_duration_mn   (+ (* 6 910) (* 4 337))
            :aggregated_duration_mn   (+ (* 6 910) (* 4 337))
          }
       "RAM"
        { :cloud_vm_instanceid      "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"        
          :aggregated_duration_mn   13872
        }
      }
     }]    
    (summarize-records [record-1 record-2 record-3] start-day end-day)))

  (is (=
    [{ 
    :user                "joe"
    :cloud               "aws"
    :start_timestamp     start-day   
    :end_timestamp       end-day
    :usage 
      {
        "Disk"
          {
            :cloud_vm_instanceid      "aws:445623"                      
            :aggregated_duration_mn   144000
          }
      }
     }]
     (summarize-records [record-4] start-day end-day)))

  (is (=
    [{ 
    :user                "joe"
    :cloud               "aws"
    :start_timestamp     start-day   
    :end_timestamp       end-day
    :usage 
      {
        "Disk"
          {
            :cloud_vm_instanceid      "aws:445623"                      
            :aggregated_duration_mn   144000
          }
      }
     }]
     (summarize-records [record-5] start-day end-day)))
  )

(defn insert-record   
  []
  (rc/-insertStart
    { :cloud_vm_instanceid   "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
      :user                "sixsq_dev"
      :cloud               "exoscale-ch-gva"
      :start_timestamp     in-day-1
      :metrics [{   :name  "nb-cpu"
                    :value 4 }
                  { :name  "RAM-GB"
                    :value 8 }
                  { :name  "disk-GB"
                    :value 100.5 }]})
  (rc/-insertEnd
    { :cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"    
      :end_timestamp       in-day-2}))

(deftest test-summarize
  (insert-record)
  (is (=
    [{ 
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     start-day   
    :end_timestamp       end-day
    :usage 
      {
        "nb-cpu"
        {
          :cloud_vm_instanceid      "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"                      
          :aggregated_duration_mn   (* 4.0 337)
        }
        "RAM-GB"
        {
          :cloud_vm_instanceid      "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"                      
          :aggregated_duration_mn   (* 8.0 337)
        }
        "disk-GB"
        {
          :cloud_vm_instanceid      "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"                      
          :aggregated_duration_mn   (* 100.5 337)
        }
      }
     }]
    (summarize start-day end-day)))
  )

(deftest test-summarize-and-store  
  (insert-record)
  (summarize-and-store start-day end-day)
  (let [summaries-from-db (select usage-summaries)]
    (is (= 1 (count summaries-from-db)))
    (is (= "{\"disk-GB\":\n {\"cloud_vm_instanceid\":\n  \"exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592\",\n  \"aggregated_duration_mn\":33868.5},\n \"RAM-GB\":\n {\"cloud_vm_instanceid\":\n  \"exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592\",\n  \"aggregated_duration_mn\":2696.0},\n \"nb-cpu\":\n {\"cloud_vm_instanceid\":\n  \"exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592\",\n  \"aggregated_duration_mn\":1348.0}}\n"
       (:usage (first summaries-from-db))))))


