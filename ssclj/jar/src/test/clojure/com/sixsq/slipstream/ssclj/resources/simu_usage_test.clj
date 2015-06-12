(ns com.sixsq.slipstream.ssclj.resources.simu-usage-test
  (:require
    [clojure.test                                       :refer :all]
    [clojure.java.shell                                 :only [sh] :as sh]
    [com.sixsq.slipstream.ssclj.resources.usage-record  :refer :all]
    [com.sixsq.slipstream.ssclj.usage.record-keeper     :as rc]
    [korma.core                                         :as kc]

    [com.sixsq.slipstream.ssclj.usage.summary           :as us]

    [com.sixsq.slipstream.ssclj.api.acl                 :as acl]

    [clj-time.core                                      :as time]
    [com.sixsq.slipstream.ssclj.usage.utils             :as uu]
    [com.sixsq.slipstream.ssclj.resources.test-utils    :as tu]
    [com.sixsq.slipstream.ssclj.resources.common.utils  :as cu]))

(def nb-users           10)
(def nb-clouds          10)
(def start-day          [2010])

;; parameters to tweak
(def nb-days            100)
(def nb-records-per-day 100)

(defn some-strings
  [s n]
  (->> (range)
       (map #(str s "-" %))
       (take n)))

(def some-users   (some-strings "user" nb-users))
(def some-clouds  (some-strings "cloud" nb-clouds))
(def some-metrics [{:name "vm" :value "1.0"}
                   {:name "vm" :value "2.0"}
                   {:name "vm" :value "3.0"}
                   {:name "RAM" :value "16.0"}
                   {:name "RAM" :value "32.0"}
                   {:name "DISK" :value "100.0"}
                   {:name "DISK" :value "200.0"}])

(defn rand-user   [] (rand-nth some-users))
(defn rand-cloud  [] (rand-nth some-clouds))
(defn rand-metric [] (rand-nth some-metrics))

(defn some-days
  []
  (take nb-days (iterate uu/inc-day (apply time/date-time start-day))))

(defn usage-record
  [user cloud metric start-timestamp end-timestamp]
  {
   :cloud_vm_instanceid    (str cloud ":" (cu/random-uuid))
   :user                   user
   :cloud                  cloud
   :start_timestamp        start-timestamp
   :end_timestamp          end-timestamp
   :metric_name            (:name metric)
   :metric_value           (:value metric) })

(defn rand-usage-record
  "Returns a random usage record inside the given date"
  [date]
  (let [nb-minutes-day  (* 24 60)
        minute-start    (rand-int nb-minutes-day)
        minute-end      (+ minute-start (rand-int (- nb-minutes-day minute-start)))
        start           (uu/to-ISO-8601 (uu/inc-minutes date minute-start))
        end             (uu/to-ISO-8601 (uu/inc-minutes date minute-end))]

    (usage-record (rand-user) (rand-cloud) (rand-metric) start end)))

(defn compute-summaries
  [days]
  (acl/-init)
  (rc/-init)
  (->> days
       (map uu/to-ISO-8601)
       (partition 2 1)
       (map (fn[[s e]] (us/summarize-and-store s e)))
       dorun))

;;
;; public
;;

(defn delete-everything
  []
  (acl/-init)
  (rc/-init)

  (kc/delete rc/usage_records)
  (kc/delete rc/usage_summaries)
  (acl/delete-all))

(defn populate-usage-records
  []
  (rc/-init)
  (doseq [day (some-days)]
    (println "Populating " day)
    (doseq [i (range nb-records-per-day)]
      (kc/insert rc/usage_records (kc/values (rand-usage-record day))))))


(defn compute-daily-summaries
  ;;
  ;; currently done daily by a cron job
  ;;
  []
  (compute-summaries (some-days)))

;;
;; user-1 for the day january 21st 2015:
;;
;; ?$filter=user='user-3' and start_timestamp='2010-02-10T00:00:00.000Z' and end_timestamp='2010-02-11T00:00:00.000Z'
;;
;; cloud-2
;;
;; ?$filter=cloud='cloud-2' and start_timestamp='2010-02-10T00:00:00.000Z' and end_timestamp='2010-02-11T00:00:00.000Z'

(defn curl
  [qs]
  (println
    (str "time curl -H \"slipstream-authn-info: super ADMIN\" -X GET \"http://localhost:8201/api/Usage"
       (tu/urlencode-params qs) "\"")))