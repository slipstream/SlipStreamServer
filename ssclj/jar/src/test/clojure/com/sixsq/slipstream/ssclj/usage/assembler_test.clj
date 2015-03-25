(ns com.sixsq.slipstream.ssclj.usage.assembler-test
  (:require    
    [com.sixsq.slipstream.ssclj.usage.assembler :as a]     
    [com.sixsq.slipstream.ssclj.usage.utils :as u]
    [clojure.test :refer :all]))

(def u1 { :id "joe"
          :event :start 
          :timestamp (u/to-time "2015-01-16T08:20:00.0Z")
          :dimensions {:nb-cpu 2}})

(def u2  { :id "joe"
          :event :end 
          :timestamp (u/to-time "2015-01-16T09:44:12.0Z")})

(def u3 { :id "wrong-order"
          :event :end 
          :timestamp (u/to-time "2015-01-15T07:20:16.0Z")})

(def u4 { :id "wrong-order"
          :event :start 
          :timestamp (u/to-time "2015-01-16T07:20:16.0Z")
          :dimensions {:nb-cpu 1}})

(def u5 { :id "joe"
          :event :start 
          :timestamp (u/to-time "2015-01-17T08:20:00.0Z")
          :dimensions {:nb-cpu 1}})

(def u6  { :id "joe"
          :event :end 
          :timestamp (u/to-time "2015-01-17T12:44:12.0Z")})

(def u7 { :id "jack"
          :event :start 
          :timestamp (u/to-time "2015-01-17T08:20:00.0Z")
          :dimensions {:nb-cpu 1}})

(def u8  { :id "jack"
          :event :end 
          :timestamp (u/to-time "2015-01-17T12:44:12.0Z")})


(def block-u1-u2
  { :id "joe"    
    :start-timestamp (u/to-time "2015-01-16T08:20:00.0Z")
    :end-timestamp (u/to-time "2015-01-16T09:44:12.0Z")
    :dimensions {:nb-cpu 2}})

(def block-u1-open
  { :id "joe"    
    :start-timestamp (u/to-time "2015-01-16T08:20:00.0Z")
    :end-timestamp nil
    :dimensions {:nb-cpu 2}})

(deftest build-block
  (is (= block-u1-u2 (a/build-block [u1 u2]))))

(deftest build-open-ended-block
  (is (= block-u1-open (a/build-block [u1 nil]))))

(deftest build-block-different-id
  (is (thrown? IllegalArgumentException (a/build-block [u1 u3]))))

(deftest build-block-end-start
  (is (thrown? IllegalArgumentException (a/build-block [u2 u1]))))

(deftest detect-2-consecutives-starts-or-ends
  (is (thrown? IllegalArgumentException (a/build-blocks "joe" [u1 u1 u2])))
  (is (thrown? IllegalArgumentException (a/build-blocks "joe" [u1 u2 u1])))
  (is (thrown? IllegalArgumentException (a/build-blocks "joe" [u2 u2 u1])))
  (is (thrown? IllegalArgumentException (a/build-blocks "joe" [u1 u2 u2]))))

(deftest detect-first-is-end
  (is (not (nil? (a/build-blocks "joe" [u2 u1]))))
  (is (not (nil? (a/build-blocks "joe" [u1 u2]))))

  (is (thrown? IllegalArgumentException (a/build-blocks "wrong-order" [u2 u1])))
  (is (thrown? IllegalArgumentException (a/build-blocks "wrong-order" [u1 u2]))))

(deftest build-blocks
  (is (= [block-u1-u2] (a/build-blocks "joe" [u1 u2])))
  (is (= [block-u1-u2] (a/build-blocks "joe" [u1 u2 u7 u8]))))


