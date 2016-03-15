(ns com.sixsq.slipstream.ssclj.usage.record-keeper-test
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.ssclj.usage.record-keeper :refer :all]
    [korma.core :refer :all]
    [clojure.tools.logging :as log]
    [clojure.test :refer :all]))

(def start-day-0 "2015-01-15T00:00:00.0Z")
(def end-day-0 "2015-01-16T00:00:00.0Z")

(def event-start-time "2015-01-16T09:44:12.0Z")
(def middle-event "2015-01-16T12:00:00.0Z")
(def event-end-time "2015-01-16T14:38:43.0Z")

(def start-day-1 "2015-01-16T00:00:00.0Z")
(def end-day-1 "2015-01-17T00:00:00.0Z")

(def start-day-2 "2015-01-17T00:00:00.0Z")
(def end-day-2 "2015-01-18T00:00:00.0Z")

(defn delete-all [f]
  (-init)
  (delete usage_records)
  (log/debug "All usage_records deleted")
  (log/debug "usage records " (select usage_records))
  (f))
(use-fixtures :each delete-all)

(def event-start
  {:cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :user                "sixsq_dev"
   :cloud               "exoscale-ch-gva"
   :start_timestamp     event-start-time
   :metrics             [{:name  "nb-cpu"
                          :value 4}
                         {:name  "RAM-GB"
                          :value 8}
                         {:name  "disk-GB"
                          :value 100.5}]})

(def event-end
  {:cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :end_timestamp       event-end-time
   :metrics             [{:name "nb-cpu"}
                         {:name "RAM-GB"}
                         {:name "disk-GB"}]})

(def event-starting-day-2
  (-> event-start
      (assoc :start_timestamp start-day-2)
      (assoc :metrics [{:name  "nb-cpu"
                        :value 8}
                       {:name  "RAM-GB"
                        :value 16}
                       {:name  "disk-GB"
                        :value 300}])))

(def event-start-small-vm1
  (-> event-start
      (assoc :cloud_vm_instanceid "exoscale-ch-gva:vm1")
      (assoc :metrics [{:name  "instance-type.Small" :value 1}
                       {:name  "nb-cpu"              :value 8}])))

(def event-start-small-vm2
  (-> event-start-small-vm1
      (assoc :cloud_vm_instanceid "exoscale-ch-gva:vm2")))

(def event-change-to-large
  (-> event-start-small-vm1
      (assoc :start_timestamp end-day-2)
      (assoc :metrics [{:name  "instance-type.Large" :value 1}])))

(def event-ending-day-2
  (assoc event-start :end_timestamp end-day-2))

(deftest basic-insert-open
  (-insertStart event-start)
  (let [records (select usage_records)]
    (is (= 3 (count records)))
    (is (= event-start-time (:start_timestamp (first records))))
    (is (nil? (:end_timestamp (first records))))))

(deftest basic-insert
  (-insertStart event-start)
  (-insertEnd event-end)
  (let [records (select usage_records)]
    (is (= 3 (count records)))
    (is (= event-start-time (:start_timestamp (first records))))
    (is (= event-end-time (:end_timestamp (first records))))))

(deftest inserts-should-keywordize-maps
  (let [start-with-string-keys
        {"cloud_vm_instanceid" "exo:123"
         "user"                "sixsq_dev"
         "cloud"               "exo"
         "start_timestamp"     event-start-time
         "metrics"             [{"name" "A" "value" 4} {"name" "B" "value" 8}]}
        end-with-string-keys
        {"cloud_vm_instanceid" "exo:123"
         "end_timestamp"       event-end-time
         "metrics"             [{"name" "A"} {"name" "B"}]}]
    (-insertStart start-with-string-keys)
    (-insertEnd end-with-string-keys)
    (is (= 2 (count (records-for-interval start-day-1 end-day-1))))
    (is (zero? (count (records-for-interval start-day-2 end-day-2))))))

(deftest records-for-interval-for-open-records
  (-insertStart event-start)
  (log/debug "after insert, usage records " (select usage_records))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-1 end-day-1))))
  (is (zero? (count (records-for-interval start-day-0 end-day-0))))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-2 end-day-2)))))

(deftest records-for-interval-for-closed-records
  (-insertStart event-start)
  (is (every? nil? (map :end_timestamp (records-for-interval start-day-0 end-day-2))))
  (is (= 3 (count (records-for-interval start-day-0 end-day-2))))
  (-insertEnd event-end)
  (is (every? #(= event-end-time %) (map :end_timestamp (records-for-interval start-day-0 end-day-2))))
  (is (zero? (count (records-for-interval start-day-2 end-day-2)))))

(deftest invalid-date
  (is (thrown? IllegalArgumentException (records-for-interval end-day-2 start-day-0))))

(deftest event-already-open-can-be-reopened-but-doeexoscale-ch-gva:fe3c02d4-c8f9-427a-a8f8-897e422edd2cs-nothing
  (is (empty? (select usage_records)))
  (-insertStart event-start)
  (let [records (select usage_records)]
    (is (= 3 (count records)))
    (-insertStart event-starting-day-2)
    (is (= records (select usage_records)))))

(deftest same-metric-can-be-open-close-multiple-times-when-scaling
  (is (empty? (select usage_records)))

  (-insertStart event-start)
  (-insertEnd event-end)
  (-insertStart event-starting-day-2)
  (-insertEnd event-ending-day-2)

  (is (= (set
           [["nb-cpu" 4.0 event-start-time event-end-time]
            ["RAM-GB" 8.0 event-start-time event-end-time]
            ["disk-GB" 100.5 event-start-time event-end-time]
            ["nb-cpu" 8.0 start-day-2 end-day-2]
            ["RAM-GB" 16.0 start-day-2 end-day-2]
            ["disk-GB" 300.0 start-day-2 end-day-2]])
         (set
           (map
             #((juxt :metric_name :metric_value :start_timestamp :end_timestamp) %)
             (select usage_records))))))

(deftest check-close-event-without-start
  (-insertEnd event-end)
  (is (zero? (count (select usage_records)))))

(deftest check-with-middle-time
  (-insertStart event-start)
  (-insertEnd event-end)
  (is (= 3 (count (records-for-interval middle-event end-day-2))))
  (is (= 3 (count (records-for-interval start-day-0 middle-event)))))

(deftest close-instance-type-when-changed-for-same-vm
  (-insertStart event-start-small-vm1)
  (-insertStart event-start-small-vm2)
  (-insertStart event-change-to-large)

  (is (= (:start_timestamp event-change-to-large)
         (->> (select usage_records (where {:cloud_vm_instanceid "exoscale-ch-gva:vm1"
                                            :metric_name         "instance-type.Small"}))
              first
              :end_timestamp)))
  (is (nil? (->> (select usage_records (where {:cloud_vm_instanceid "exoscale-ch-gva:vm1"
                                               :metric_name         "nb-cpu"}))
                 first
                 :end_timestamp)))
  (is (nil? (->> (select usage_records (where {:cloud_vm_instanceid "exoscale-ch-gva:vm2"}))
                 first
                 :end_timestamp))))
