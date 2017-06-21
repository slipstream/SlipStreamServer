(ns com.sixsq.slipstream.ssclj.usage.summary-test
  (:refer-clojure :exclude [update])
  (:require
    [com.sixsq.slipstream.ssclj.usage.summary :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as tu]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))

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

(use-fixtures :each tu/with-test-client-fixture)

(deftest truncate-filters-outside-records
  (let [urs [{:start-timestamp in-day-1 :end-timestamp in-day-2}]]
    (is (= urs (truncate start-day end-day urs)))
    (is (= urs (truncate past-1 future-1 urs)))
    (is (= [] (truncate past-1 past-2 urs)))
    (is (= [] (truncate future-1 future-2 urs)))))

(deftest truncate-checks-args
  (truncate past-1 past-2 [])
  (is (thrown? IllegalArgumentException (truncate past-2 past-1 []))))

(deftest truncate-move-start
  (let [urs [{:start-timestamp past-1 :end-timestamp in-day-2}]]
    (is (= [{:start-timestamp start-day :end-timestamp in-day-2}]
           (truncate start-day end-day urs)))))

(deftest truncate-move-end
  (let [urs [{:start-timestamp in-day-1 :end-timestamp future-1}]]
    (is (= [{:start-timestamp in-day-1 :end-timestamp end-day}]
           (truncate start-day end-day urs)))))

(deftest test-contribution
  (is (= 11692.8 (contribution {:start-timestamp start-day :end-timestamp end-day :metric-value 8.12}))))

(def record-1
  {:cloud-vm-instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :user                "sixsq_dev"
   :cloud               "exoscale-ch-gva"
   :start-timestamp     in-day-1
   :end-timestamp       in-day-2
   :metric-name         "nb-cpu"
   :metric-value        "4"})

(def record-2
  {:cloud-vm-instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :user                "sixsq_dev"
   :cloud               "aws"
   :start-timestamp     start-day
   :end-timestamp       in-day-2
   :metric-name         "nb-cpu"
   :metric-value        "6"})

(def record-3
  {:cloud-vm-instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
   :user                "sixsq_dev"
   :cloud               "exoscale-ch-gva"
   :start-timestamp     in-day-1
   :end-timestamp       end-day
   :metric-name         "RAM"
   :metric-value        "16"})

(def record-4
  {:cloud-vm-instanceid "aws:445623"
   :user                "joe"
   :cloud               "aws"
   :start-timestamp     past-1
   :end-timestamp       future-2
   :metric-name         "Disk"
   :metric-value        "100"})

(def record-5
  {:cloud-vm-instanceid "aws:445623"
   :user                "joe"
   :cloud               "aws"
   :start-timestamp     past-1
   :end-timestamp       nil
   :metric-name         "Disk"
   :metric-value        "100"})

(deftest test-summarize-records
  (is (=
        [{
          :user            "sixsq_dev"
          :cloud           "exoscale-ch-gva"
          :start-timestamp start-day
          :end-timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "nb-cpu"
                            {
                             :unit-minutes 1348.0
                             }
                            "RAM"
                            {
                             :unit-minutes 13872.0
                             }
                            }
          }
         {
          :user            "sixsq_dev"
          :cloud           "aws"
          :start-timestamp start-day
          :end-timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "nb-cpu"
                            {
                             :unit-minutes 5460.0
                             }
                            }
          }
         ]
        (summarize-records [record-1 record-2 record-3] start-day end-day :daily [:user :cloud])))

  (is (=
        [{
          :user            "joe"
          :cloud           "aws"
          :start-timestamp start-day
          :end-timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "Disk"
                            {
                             :unit-minutes 144000.0
                             }
                            }
          }]
        (summarize-records [record-4] start-day end-day :daily [:user :cloud])))

  (is (=
        [{
          :user            "joe"
          :cloud           "aws"
          :start-timestamp start-day
          :end-timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage
                           {
                            "Disk"
                            {
                             :unit-minutes 144000.0
                             }
                            }
          }]
        (summarize-records [record-5] start-day end-day :daily [:user :cloud])))
  )

(defn insert-record
  []
  (rc/insert-usage-event
    {:acl                 {:owner {:type "USER" :principal "sixsq_dev"}
                           :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
     :cloud-vm-instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
     :user                "sixsq_dev"
     :cloud               "exoscale-ch-gva"
     :start-timestamp     in-day-1
     :metrics             [{:name  "nb-cpu"
                            :value 4}
                           {:name  "RAM-GB"
                            :value 8}
                           {:name  "disk-GB"
                            :value 100.5}]} {:user-roles ["ADMIN"]})
  (rc/insert-usage-event
    {:acl                 {:owner {:type "USER" :principal "sixsq_dev"}
                           :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
     :cloud-vm-instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
     :start-timestamp     in-day-1
     :end-timestamp       in-day-2
     :metrics             [{:name "nb-cpu"}
                           {:name "RAM-GB"}
                           {:name "disk-GB"}]} {:user-roles ["ADMIN"]}))

(deftest test-summarize
  (insert-record)
  (is (=
        [{
          :user            "sixsq_dev"
          :cloud           "exoscale-ch-gva"
          :start-timestamp start-day
          :end-timestamp   end-day
          :frequency       "daily"
          :grouping        "user,cloud"
          :usage           {"nb-cpu"  {:unit-minutes (* 4.0 337)}
                            "RAM-GB"  {:unit-minutes (* 8.0 337)}
                            "disk-GB" {:unit-minutes (* 100.5 337)}}
          }]
        (summarize start-day end-day :daily [:user :cloud]))))


(defn- summaries-from-db
  []
  (second (db/query "UsageSummary" {:user-roles ["ADMIN"]})))

(deftest test-summarize-and-store-by-user-cloud
  (insert-record)
  (summarize-and-store! start-day end-day :daily [:user :cloud])
  (let [summaries-from-db (summaries-from-db)
        result {:disk-GB {:unit-minutes 33868.5},
                :RAM-GB  {:unit-minutes 2696.0},
                :nb-cpu  {:unit-minutes 1348.0}}]
    (is (= 1 (count summaries-from-db)))
    (let [usage (first summaries-from-db)]
      (is (= result (:usage usage)))
      (is (= "daily" (:frequency usage)))
      (is (= "user,cloud" (:grouping usage))))))

(deftest test-summarize-and-store-by-cloud
  (insert-record)
  (summarize-and-store! start-day end-day :daily [:cloud])
  (let [summaries-from-db (summaries-from-db)
        result {:disk-GB {:unit-minutes 33868.5},
                :RAM-GB  {:unit-minutes 2696.0},
                :nb-cpu  {:unit-minutes 1348.0}}]
    (is (= 1 (count summaries-from-db)))
    (let [usage (first summaries-from-db)]
      (is (= result (:usage usage)))
      (is (= "daily" (:frequency usage)))
      (is (= "cloud" (:grouping usage))))))

(deftest summarize-records-by-cloud
  (is (= [{
           :cloud           "exoscale-ch-gva"
           :start-timestamp start-april
           :end-timestamp   start-may
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"nb-cpu" {:unit-minutes 1348.0}
                             "RAM"    {:unit-minutes 13872.0}}
           }
          {:cloud           "aws"
           :start-timestamp start-april
           :end-timestamp   start-may
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"Disk"   {:unit-minutes 4176000.0}
                             "nb-cpu" {:unit-minutes 5460.0}}}]
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily [:cloud]))))

(deftest summarize-records-by-cloud-except-users
  (is (= []
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily
                            [:cloud] ["sixsq_dev" "joe"])))
  (is (= [{:cloud           "aws"
           :end-timestamp   "2015-05-01T00:00:00.000Z"
           :start-timestamp "2015-04-01T00:00:00.000Z"
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"Disk" {:unit-minutes 4176000.0}}}]
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily
                            [:cloud] ["sixsq_dev"])))
  (is (= [{:cloud           "exoscale-ch-gva"
           :end-timestamp   "2015-05-01T00:00:00.000Z"
           :start-timestamp "2015-04-01T00:00:00.000Z"
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"RAM"    {:unit-minutes 13872.0}
                             "nb-cpu" {:unit-minutes 1348.0}}}
          {:cloud           "aws"
           :end-timestamp   "2015-05-01T00:00:00.000Z"
           :start-timestamp "2015-04-01T00:00:00.000Z"
           :frequency       "daily"
           :grouping        "cloud"
           :usage           {"nb-cpu" {:unit-minutes 5460.0}}}]
         (summarize-records [record-1 record-2 record-3 record-4 record-5] start-april start-may :daily
                            [:cloud] ["joe"]))))
