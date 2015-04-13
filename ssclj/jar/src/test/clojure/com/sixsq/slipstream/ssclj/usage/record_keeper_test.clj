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
  (-init)
  (delete usage-records)
  (log/debug "All usage-records deleted")
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

(deftest inserts-should-keywordize-maps
  (let [start-with-string-keys
        { "cloud_vm_instanceid" "exo:123"
          "user"                "sixsq_dev"
          "cloud"               "exo"
          "start_timestamp"     event-start-time
          "metrics" [{"name"  "A" "value" 4 } {"name" "B" "value" 8}]}
        end-with-string-keys
        { "cloud_vm_instanceid" "exo:123"          
          "end_timestamp"     event-end-time }]
    (-insertStart start-with-string-keys)
    (-insertEnd end-with-string-keys)
    (is (= 2 (count (records-for-interval start-day-1 end-day-1))))  
    (is (= 0 (count (records-for-interval start-day-2 end-day-2))))))

(deftest records-for-interval-for-open-records
  (-insertStart event-start)
  (log/debug "after insert, usage records " (select usage-records))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-1 end-day-1))))
  (is (= 0 (count (records-for-interval start-day-0 end-day-0))))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-2 end-day-2)))))

(deftest records-for-interval-for-closed-records  
  (-insertStart event-start)
  (is (every? nil? (map :end_timestamp (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (-insertEnd   event-end)
  (is (every? #(= event-end-time %) (map :end_timestamp (records-for-interval start-day-0 end-day-2))))
  (is (= 0 (count (records-for-interval start-day-2 end-day-2)))))

(deftest invalid-date
  (is (thrown? IllegalArgumentException (records-for-interval end-day-2 start-day-0))))

(defn todo [] (is (= :done :not-yet)))

(deftest event-already-open-can-be-reopened-but-does-nothing
  (is (empty? (select usage-records)))
  (-insertStart event-start)
  (let [records (select usage-records)]
    (-insertStart event-start)
    (is (= records (select usage-records)))))

(deftest check-close-event-without-start
  (is (thrown? IllegalArgumentException (-insertEnd event-end))))

(deftest check-with-middle-time
  (-insertStart event-start)
  (-insertEnd   event-end)
  (is (= 3 (count (records-for-interval middle-event end-day-2))))
  (is (= 3 (count (records-for-interval start-day-0 middle-event)))))
