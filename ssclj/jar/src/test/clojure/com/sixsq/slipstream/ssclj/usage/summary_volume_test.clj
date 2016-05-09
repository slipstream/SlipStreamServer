(ns com.sixsq.slipstream.ssclj.usage.summary-volume-test
  (:refer-clojure :exclude [update])
  (:require
    [korma.core :as kc]
    [clojure.test :refer :all]
    [clj-time.core :as t]
    [com.sixsq.slipstream.ssclj.usage.summary :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.db.impl :as db]))

(use-fixtures :each ltu/flush-db-fixture)

(def jack-exoscale
  {
   :user    "jack"
   :cloud   "exoscale-ch-gva"
   :metrics [{:name  "small"
              :value 2.0}
             {:name  "big"
              :value 4.0}]})

(def joe-exoscale
  {
   :user    "joe"
   :cloud   "exoscale-ch-gva"
   :metrics [{:name  "nb-cpu"
              :value 2.0}
             {:name  "RAM-GB"
              :value 8.0}
             {:name  "disk-GB"
              :value 100.5}]})

(def joe-aws
  {
   :user    "joe"
   :cloud   "aws"
   :metrics [{:name  "nb-cpu"
              :value 4.0}
             {:name  "RAM-GB"
              :value 16.0}
             {:name  "disk-GB"
              :value 510.0}]})

(def event-end-template
  {:cloud-vm-instanceid "exoscale-ch-gva:abcd"})

(defn fill-joe
  [nb-days]
  (doseq [day (range nb-days)]
    (let [start-day     (t/plus (t/date-time 2014) (t/days day))
          day-9h        (t/plus start-day (t/hours 9))
          day-11h       (t/plus start-day (t/hours 11))
          day-13h       (t/plus start-day (t/hours 13))
          day-14h       (t/plus start-day (t/hours 14))
          vm-joe-exo-id (str "exo" day)
          vm-joe-aws-id (str "aws" day)

          joe-exo-start (assoc joe-exoscale
                          :start-timestamp (u/to-ISO-8601 day-9h)
                          :cloud-vm-instanceid vm-joe-exo-id)
          joe-exo-end   {:end-timestamp       (u/to-ISO-8601 day-14h)
                         :cloud-vm-instanceid vm-joe-exo-id
                         :metrics             [{:name "nb-cpu"}
                                               {:name "RAM-GB"}
                                               {:name "disk-GB"}]}

          joe-aws-start (assoc joe-aws
                          :start-timestamp (u/to-ISO-8601 day-11h)
                          :cloud-vm-instanceid vm-joe-aws-id)
          joe-aws-end   {:end-timestamp       (u/to-ISO-8601 day-13h)
                         :cloud-vm-instanceid vm-joe-aws-id
                         :metrics             [{:name "nb-cpu"}
                                               {:name "RAM-GB"}
                                               {:name "disk-GB"}]}

          ]
      (rc/insert-usage-event joe-exo-start {})
      (rc/insert-usage-event joe-aws-start {})

      (rc/insert-usage-event joe-exo-end {})
      (rc/insert-usage-event joe-aws-end {}))))

(defn summarize-joe-weekly
  [nb-weeks]
  (doseq [i (range nb-weeks)]
    (let [start-week (t/plus (t/date-time 2014) (t/weeks i))
          end-week   (t/plus start-week (t/weeks 1))]
      (summarize-and-store! (u/to-ISO-8601 start-week) (u/to-ISO-8601 end-week) :daily [:user :cloud]))))

(defn- summaries-from-db
  []
  (db/query "usage" {:user-roles ["ADMIN"]}))

(defn extract-data
  [usage]
  (-> usage :data u/deserialize))

(defn extract-summary
  [usage]
  (-> usage :data u/deserialize :usage u/deserialize))

(defn extract-user
  [usage]
  (-> usage :data u/deserialize :user))

(defn check-summaries
  []
  (doseq [summary (->> (summaries-from-db) (map extract-data))]
    (if (= "exoscale-ch-gva" (:cloud summary))
      (do
        (is (= "joe" (:user summary)))
        (is (= {:disk-GB {:unit-minutes 211050.0},
                         :RAM-GB  {:unit-minutes 16800.0},
                         :nb-cpu  {:unit-minutes 4200.0}} (u/deserialize (:usage summary)))))
      (do
        (is (= "aws" (:cloud summary)))
        (is (= "joe" (:user summary)))
        (is (= {:disk-GB {:unit-minutes 428400.0},
                         :RAM-GB  {:unit-minutes 13440.0},
                         :nb-cpu  {:unit-minutes 3360.0}} (u/deserialize (:usage summary))))))))

(deftest check-precision-small-interval
  (let [start-day           (t/date-time 2014)
        end-day             (t/plus (t/date-time 2014) (t/days 1))

        fifty-seconds-after (t/plus start-day (t/seconds 50))

        joe-exo-start       (assoc joe-exoscale
                              :start-timestamp (u/to-ISO-8601 start-day)
                              :cloud-vm-instanceid "vm-joe-exo-id")
        joe-exo-end         {:end-timestamp       (u/to-ISO-8601 fifty-seconds-after)
                             :cloud-vm-instanceid "vm-joe-exo-id"
                             :metrics             [{:name "nb-cpu"}
                                                   {:name "RAM-GB"}
                                                   {:name "disk-GB"}]
                             }]

    (rc/insert-usage-event joe-exo-start {})))
    ;(rc/insert-usage-event joe-exo-end {})
    ;(summarize-and-store! (u/to-ISO-8601 start-day) (u/to-ISO-8601 end-day) :daily [:user :cloud])
    ;
    ;(let [summary (-> (summaries-from-db)
    ;                  first
    ;                  extract-summary)]
    ;  (is (= 83.75 (get-in summary [:disk-GB :unit-minutes])))
    ;  (is (< 6.666 (get-in summary [:RAM-GB :unit-minutes]) 6.667))
    ;  (is (< 1.666 (get-in summary [:nb-cpu :unit-minutes]) 1.667)))))


(deftest test-with-records-full-year
  (let [nb-weeks 5]
    (fill-joe (* nb-weeks 7))
    (summarize-joe-weekly nb-weeks)
    (check-summaries)))

(deftest multiple-users-same-cloud
  (let [start-day      (t/date-time 2014)
        end-day        (t/plus start-day (t/days 1))

        joe-exo-start  (assoc joe-exoscale
                         :start-timestamp (u/to-ISO-8601 start-day)
                         :cloud-vm-instanceid "vm-joe-exo-id")
        joe-exo-end    {:end-timestamp       (u/to-ISO-8601 end-day)
                        :cloud-vm-instanceid "vm-joe-exo-id"}

        jack-exo-start (assoc jack-exoscale
                         :start-timestamp (u/to-ISO-8601 start-day)
                         :cloud-vm-instanceid "vm-jack-exo-id")
        jack-exo-end   {:end-timestamp       (u/to-ISO-8601 end-day)
                        :cloud-vm-instanceid "vm-jack-exo-id"}]

    (rc/insert-usage-event joe-exo-start {})
    (rc/insert-usage-event jack-exo-start {})
    (rc/insert-usage-event joe-exo-end {})
    (rc/insert-usage-event jack-exo-end {})

    (summarize-and-store! (u/to-ISO-8601 start-day) (u/to-ISO-8601 end-day) :daily [:user :cloud])

    (let [summaries (summaries-from-db)]
      (is (= [
              {:disk-GB {:unit-minutes 144720.0}, :RAM-GB {:unit-minutes 11520.0}, :nb-cpu {:unit-minutes 2880.0}}]
             (map extract-summary (filter #(= "joe" (extract-user %)) summaries))))
      (is (= [
              {:big {:unit-minutes 5760.0}, :small {:unit-minutes 2880.0}}]
             (map extract-summary (filter #(= "jack" (extract-user %)) summaries)))))))

(deftest summarize-open-record
  (let [start-day     (t/date-time 2014)
        end-day       (t/plus (t/date-time 2014) (t/days 1))
        joe-exo-start (assoc joe-exoscale
                        :start-timestamp (u/to-ISO-8601 start-day)
                        :cloud-vm-instanceid "vm-joe-exo-id")]

    (rc/insert-usage-event joe-exo-start {})

    (summarize-and-store! (u/to-ISO-8601 (t/minus start-day (t/days 1))) (u/to-ISO-8601 end-day) :daily [:user :cloud])

    (let [summaries (summaries-from-db)]
      (is (= [{:disk-GB {:unit-minutes 144720.0}, :RAM-GB {:unit-minutes 11520.0}, :nb-cpu {:unit-minutes 2880.0}}]
             (map extract-summary summaries))))))

(deftest multiple-summaries-on-same-per-user-cloud-interval-are-ok
  (let [start-day     (t/date-time 2014)
        end-day       (t/plus start-day (t/days 1))
        joe-exo-start (assoc joe-exoscale
                        :start-timestamp (u/to-ISO-8601 start-day)
                        :cloud-vm-instanceid "vm-joe-exo-id")]

    (rc/insert-usage-event joe-exo-start {})

    (summarize-and-store! (u/to-ISO-8601 start-day) (u/to-ISO-8601 end-day) :daily [:user :cloud])
    (summarize-and-store! (u/to-ISO-8601 start-day) (u/to-ISO-8601 end-day) :daily [:user :cloud])
    (is (= 2 (count (summaries-from-db))))))


