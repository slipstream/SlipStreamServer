(ns com.sixsq.slipstream.ssclj.usage.record-keeper-test
  (:require 
    [com.sixsq.slipstream.ssclj.usage.record-keeper :refer :all]
    [korma.core :refer :all]
    [clojure.tools.logging :as log]
    [clojure.test :refer :all]))

(def start-day-0  "2015-01-15T00:00:00.0Z")
(def end-day-0    "2015-01-16T00:00:00.0Z")

(def event-start-time   "2015-01-16T09:44:12.0Z")
(def middle-event       "2015-01-16T12:00:00.0Z")
(def event-end-time     "2015-01-16T14:38:43.0Z")

(def start-day-1  "2015-01-16T00:00:00.0Z")
(def end-day-1    "2015-01-17T00:00:00.0Z")

(def start-day-2  "2015-01-17T00:00:00.0Z")
(def end-day-2    "2015-01-18T00:00:00.0Z")

(defn delete-all [f]
  (delete usage-records)
  (log/info "All usage-records deleted")
  (log/debug "usage records " (select usage-records))
  (f))
(use-fixtures :each delete-all)

(def event-start
  { :cloud_vm_instanceid   "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start_timestamp     event-start-time
    :metrics [{   :name  "nb-cpu"
                  :value 4 }
                { :name  "RAM-GB"
                  :value 8 }
                { :name  "disk-GB"
                  :value 100.5 }]})

(def event-end
  { :cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"    
    :end_timestamp       event-end-time})

(deftest records-for-interval-for-open-records
  (insert-start event-start)
  (log/debug "after insert, usage records " (select usage-records))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-1 end-day-1))))
  (is (= 0 (count (records-for-interval start-day-0 end-day-0))))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-2 end-day-2)))))

(deftest records-for-interval-for-closed-records  
  (insert-start event-start)
  (is (every? nil? (map :end_timestamp (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (insert-end   event-end)
  (is (every? #(= event-end-time %) (map :end_timestamp (records-for-interval start-day-0 end-day-2))))
  (is (= 0 (count (records-for-interval start-day-2 end-day-2)))))

(deftest invalid-date
  (is (thrown? IllegalArgumentException (records-for-interval end-day-2 start-day-0))))

(defn todo [] (is (= :done :not-yet)))

(deftest event-already-open-can-not-be-reopened
  (insert-start event-start)
  (is (thrown? IllegalArgumentException (insert-start event-start))))

(deftest check-close-event-without-start
  (is (thrown? IllegalArgumentException (insert-end event-end))))

(deftest check-with-middle-time
  (insert-start event-start)
  (insert-end   event-end)
  (is (= 3 (count (records-for-interval middle-event end-day-2))))
  (is (= 3 (count (records-for-interval start-day-0 middle-event)))))
  
