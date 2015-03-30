(ns com.sixsq.slipstream.ssclj.usage.summary-test
  (:require    
    [com.sixsq.slipstream.ssclj.usage.summary :refer :all]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [clojure.test :refer :all]
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

(deftest test-filter-user-cloud-terminal
  (let [records
    [
    {:user "sixsq_dev" :cloud "exoscale-ch-gva" :metric_name "nb-cpu" :metric_value 12.5}    
    {:user "sixsq_dev" :cloud "exoscale-ch-gva" :metric_name "RAM"}
    {:user "sixsq_dev" :cloud "aws" :metric_name "nb-cpu"}
    {:user "joe" :cloud "exoscale-ch-gva" :metric_name "nb-cpu"}]]

  ( is (= [{:user "sixsq_dev" :cloud "exoscale-ch-gva" :metric_name "nb-cpu" :metric_value 12.5}] 
    (filter-user-cloud-terminal "sixsq_dev" "exoscale-ch-gva" "nb-cpu" records)))  
  ( is (= [] (filter-user-cloud-terminal "sixsq_dev" "exoscale-ch-gva" "size" records)))  
  ( is (= [] (filter-user-cloud-terminal "sixsq_dev" "another-cloud" "nb-cpu" records)))
  ( is (= [] (filter-user-cloud-terminal "mike" "exoscale-ch-gva" "nb-cpu" records)))))

(def record-1  
  { :cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     in-day-1  
    :end_timestamp       in-day-2
    :metric_name        "nb-cpu"
    :metric_value       4 })

(deftest test-summarize-records
  (is (=
    [{ 
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     in-day-1  
    :end_timestamp       in-day-2
    :usages [
      {
        :cloud_vm_instanceid      "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
        :metric_name              "nb-cpu"
        ;; 337 minutes between in-day-1 and in-day-1
        :aggregated_duration_mn   (* 4 337)
      } ]
     }]    
    (summarize-records [record-1] start-day end-day))))
