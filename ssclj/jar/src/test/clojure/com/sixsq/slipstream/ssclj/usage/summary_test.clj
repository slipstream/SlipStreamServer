(ns com.sixsq.slipstream.ssclj.usage.summary-test
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.ssclj.usage.summary :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [clojure.data.json :as json]
    [clojure.test :refer :all]
    [clojure.tools.logging :as log]
    [korma.core :refer :all]
    [clj-time.format :as f]
    [clj-time.core :as t]))

(def past-1 (u/timestamp 2015 04 12))
(def past-2 (u/timestamp 2015 04 13))
(def start-day (u/timestamp 2015 04 16))
(def in-day-1 (u/timestamp 2015 04 16 9 33))
(def in-day-2 (u/timestamp 2015 04 16 15 10))
(def end-day (u/timestamp 2015 04 17))
(def future-1 (u/timestamp 2015 04 20))
(def future-2 (u/timestamp 2015 04 22))
(def start-april (u/timestamp 2015 04))
(def start-may (u/timestamp 2015 05))

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

(deftest truncate-filters-outside-records
  (let [urs [{:start_timestamp in-day-1 :end_timestamp in-day-2}]]
    (is (= urs (truncate start-day end-day urs)))
    (is (= urs (truncate past-1 future-1 urs)))
    (is (= [] (truncate past-1 past-2 urs)))
    (is (= [] (truncate future-1 future-2 urs)))))

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
  {:cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :user                "sixsq_dev"
   :cloud               "exoscale-ch-gva"
   :start_timestamp     in-day-1
   :end_timestamp       in-day-2
   :metric_name         "nb-cpu"
   :metric_value        4})

(def record-2
  {:cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :user                "sixsq_dev"
   :cloud               "aws"
   :start_timestamp     start-day
   :end_timestamp       in-day-2
   :metric_name         "nb-cpu"
   :metric_value        6})

(def record-3
  {:cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :user                "sixsq_dev"
   :cloud               "exoscale-ch-gva"
   :start_timestamp     in-day-1
   :end_timestamp       end-day
   :metric_name         "RAM"
   :metric_value        16})

(def record-4
  {:cloud_vm_instanceid "aws:445623"
   :user                "joe"
   :cloud               "aws"
   :start_timestamp     past-1
   :end_timestamp       future-2
   :metric_name         "Disk"
   :metric_value        100})

(def record-5
  {:cloud_vm_instanceid "aws:445623"
   :user                "joe"
   :cloud               "aws"
   :start_timestamp     past-1
   :end_timestamp       nil
   :metric_name         "Disk"
   :metric_value        100})

(deftest test-summarize-records
  (is (=
        [{
          :user            "sixsq_dev"
          :cloud           "exoscale-ch-gva"
          :start_timestamp start-day
          :end_timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "nb-cpu"
                            {
                             :unit_minutes 1348.0
                             }
                            "RAM"
                            {
                             :unit_minutes 13872.0
                             }
                            }
          }
         {
          :user            "sixsq_dev"
          :cloud           "aws"
          :start_timestamp start-day
          :end_timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "nb-cpu"
                            {
                             :unit_minutes 5460.0
                             }
                            }
          }
         ]
        (summarize-records [record-1 record-2 record-3] start-day end-day :daily [:user :cloud])))

  (is (=
        [{
          :user            "joe"
          :cloud           "aws"
          :start_timestamp start-day
          :end_timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "Disk"
                            {
                             :unit_minutes 144000.0
                             }
                            }
          }]
        (summarize-records [record-4] start-day end-day :daily [:user :cloud])))

  (is (=
        [{
          :user            "joe"
          :cloud           "aws"
          :start_timestamp start-day
          :end_timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "Disk"
                            {
                             :unit_minutes 144000.0
                             }
                            }
          }]
        (summarize-records [record-5] start-day end-day :daily [:user :cloud])))
  )

(defn insert-record
  []
  (rc/-insertStart
    {:cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
     :user                "sixsq_dev"
     :cloud               "exoscale-ch-gva"
     :start_timestamp     in-day-1
     :metrics             [{:name  "nb-cpu"
                            :value 4}
                           {:name  "RAM-GB"
                            :value 8}
                           {:name  "disk-GB"
                            :value 100.5}]})
  (rc/-insertEnd
    {:cloud_vm_instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
     :end_timestamp       in-day-2
     :metrics             [{:name "nb-cpu"}
                           {:name "RAM-GB"}
                           {:name "disk-GB"}]}))

(deftest test-summarize
  (insert-record)
  (is (=
        [{
          :user            "sixsq_dev"
          :cloud           "exoscale-ch-gva"
          :start_timestamp start-day
          :end_timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage           {"nb-cpu"  {:unit_minutes (* 4.0 337)}
                            "RAM-GB"  {:unit_minutes (* 8.0 337)}
                            "disk-GB" {:unit_minutes (* 100.5 337)}}
          }]
        (summarize start-day end-day :daily [:user :cloud])))
  )

(deftest test-summarize-and-store
  (insert-record)
  (summarize-and-store! start-day end-day :daily [:user :cloud])
  (let [summaries-from-db (select usage_summaries)
        result            "{\"disk-GB\":{\"unit_minutes\":33868.5},
                 \"RAM-GB\":{\"unit_minutes\":2696.0},
                 \"nb-cpu\":{\"unit_minutes\":1348.0}}"]

    (is (= 1 (count summaries-from-db)))
    (is (= (json/read-str result)
           (-> summaries-from-db
               first
               :usage
               json/read-str)))))

(deftest summarize-and-store-by-cloud
  (insert-record)
  (summarize-and-store! start-day end-day :daily [:cloud])
  (let [summaries-from-db (select usage_summaries)
        result            "{\"disk-GB\":{\"unit_minutes\":33868.5},
                 \"RAM-GB\":{\"unit_minutes\":2696.0},
                 \"nb-cpu\":{\"unit_minutes\":1348.0}}"]

    (is (= 1 (count summaries-from-db)))
    (is (= (json/read-str result)
           (-> summaries-from-db
               first
               :usage
               json/read-str)))))

(deftest summarize-records-by-cloud
  (is (= [{
           :cloud           "exoscale-ch-gva"
           :start_timestamp start-april
           :end_timestamp   start-may
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"nb-cpu" {:unit_minutes 1348.0}
                             "RAM"    {:unit_minutes 13872.0}}
           }
          {:cloud           "aws"
           :start_timestamp start-april
           :end_timestamp   start-may
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"Disk"   {:unit_minutes 4176000.0}
                             "nb-cpu" {:unit_minutes 5460.0}}}]
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily [:cloud]))))

(deftest summarize-records-by-cloud-except-users
  (is (= []
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily
                            [:cloud] ["sixsq_dev" "joe"])))
  (is (= [{:cloud           "aws"
           :end_timestamp   "2015-05-01T00:00:00.000Z"
           :start_timestamp "2015-04-01T00:00:00.000Z"
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"Disk" {:unit_minutes 4176000.0}}}]
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily
                            [:cloud] ["sixsq_dev"])))
  (is (= [{:cloud           "exoscale-ch-gva"
           :end_timestamp   "2015-05-01T00:00:00.000Z"
           :start_timestamp "2015-04-01T00:00:00.000Z"
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"RAM"    {:unit_minutes 13872.0}
                             "nb-cpu" {:unit_minutes 1348.0}}}
          {:cloud           "aws"
           :end_timestamp   "2015-05-01T00:00:00.000Z"
           :start_timestamp "2015-04-01T00:00:00.000Z"
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"nb-cpu" {:unit_minutes 5460.0}}}]
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily
                            [:cloud] ["joe"]))))
