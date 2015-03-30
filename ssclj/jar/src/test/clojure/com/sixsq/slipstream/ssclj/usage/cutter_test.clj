(ns com.sixsq.slipstream.ssclj.usage.cutter-test
  (:require    
    [com.sixsq.slipstream.ssclj.usage.cutter :as c]
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [clojure.test :refer :all]))

(def start-day-1  (u/to-time "2015-01-16T00:00:00.0Z"))
(def start-day-2  (u/to-time "2015-01-17T00:00:00.0Z"))
(def middle-day-2 (u/to-time "2015-01-17T15:00:00.0Z"))
(def end-day-2    (u/to-time "2015-01-18T00:00:00.0Z"))
(def middle-day-3 (u/to-time "2015-01-18T12:00:00.0Z"))

(def event-start-time   "2015-01-16T09:44:12.0Z")
(def middle-event       "2015-01-16T12:00:00.0Z")
(def event-end-time     "2015-01-16T14:38:43.0Z")

(def record
  { :cloud-vm-instanceid "exoscale-ch-gva:7142f7bc-f3b1-4c1c-b0f6-d770779b1592"
    :user                "sixsq_dev"
    :cloud               "exoscale-ch-gva"
    :start-timestamp     event-start-time
    :end-timestamp       event-end-time
    :metrics [{   :name  "nb-cpu"
                  :value 4 }
                { :name  "RAM-GB"
                  :value 8 }
                { :name  "disk-GB"
                  :value 100.5 }]})

(def block-joe-day-1
  { :id "joe"    
    :start-timestamp (u/to-time "2015-01-16T08:20:00.0Z")
    :end-timestamp (u/to-time "2015-01-16T09:44:12.0Z")
    :dimensions {:nb-cpu 2}})

(def block-joe-day-2
  { :id "joe"    
    :start-timestamp (u/to-time "2015-01-17T14:20:00.0Z")
    :end-timestamp (u/to-time "2015-01-17T15:44:12.0Z")
    :dimensions {:nb-cpu 2}})

(def block-joe-day-3
  { :id "joe"    
    :start-timestamp (u/to-time "2015-01-18T10:20:00.0Z")
    :end-timestamp (u/to-time "2015-01-18T16:44:12.0Z")
    :dimensions {:nb-cpu 2}})

(def block-joe-day-4
  { :id "joe"    
    :start-timestamp (u/to-time "2015-01-19T14:20:00.0Z")
    :end-timestamp (u/to-time "2015-01-19T15:44:12.0Z")
    :dimensions {:nb-cpu 2}})

(def block-joe-day-2-cut-start
  (assoc block-joe-day-2 :start-timestamp (u/to-time "2015-01-17T15:00:00.0Z")))

(def block-joe-day-2-cut-end
  (assoc block-joe-day-2 :end-timestamp middle-day-2))

(def block-joe-day-3-cut-end
  (assoc block-joe-day-3 :end-timestamp middle-day-3))


(def blocks [block-joe-day-1 block-joe-day-2 block-joe-day-3 block-joe-day-4])

(deftest check-order-dates
  (is (thrown? IllegalArgumentException (c/cut blocks start-day-2 start-day-2)))
  (is (thrown? IllegalArgumentException (c/cut blocks end-day-2 start-day-2)))) 

(deftest blocks-for-all-days
  (is (= blocks (c/cut blocks (u/to-time "2015-01-01T00:00:00.0Z") (u/to-time "2015-02-01T00:00:00.0Z")))))

(deftest no-blocks-for-period-in-past
  (is (empty? (c/cut blocks (u/to-time "2014-01-01T00:00:00.0Z") (u/to-time "2014-02-01T00:00:00.0Z")))))

(deftest no-blocks-for-period-in-future
  (is (empty? (c/cut blocks (u/to-time "2016-01-01T00:00:00.0Z") (u/to-time "2016-02-01T00:00:00.0Z")))))

(deftest blocks-for-day-2
  (is (= [block-joe-day-2] (c/cut blocks start-day-2 end-day-2))))

(deftest blocks-with-start-moved
  (is (= [block-joe-day-2-cut-start] (c/cut blocks middle-day-2 end-day-2))))

(deftest blocks-with-end-moved
  (is (= [block-joe-day-2-cut-end] (c/cut blocks start-day-2 middle-day-2))))

(deftest blocks-from-before-day-1-to-middle-day3
  (is (= [block-joe-day-1 block-joe-day-2 block-joe-day-3-cut-end] (c/cut blocks start-day-1 middle-day-3))))
  



