(ns com.sixsq.slipstream.ssclj.usage.summarizer-test
  (:require
    [com.sixsq.slipstream.ssclj.usage.summarizer :as us]
    [clj-time.core :as time]
    [clojure.test :refer :all]))

(deftest user-summary-without-date
  (us/-main "-f" "daily")
  (us/-main "-f" "weekly")
  (us/-main "-f" "monthly"))

(deftest user-summary-with-args
  (us/-main "-d" "2015-01-01" "-f" "daily")
  (us/-main "--date" "2015-01-01" "--frequency" "daily"))

(deftest user-summary-requires-frequency
  (is (thrown? IllegalArgumentException (us/-main))))

(deftest user-summary-checks-args
  (is (thrown-with-msg? IllegalArgumentException #"Unknown option" (us/-main "-x" "2015-01-01")))
  (is (thrown-with-msg? IllegalArgumentException #"Failed to validate \"-d" (us/-main "-d" "2015-01")))
  (is (thrown-with-msg? IllegalArgumentException #"Failed to validate \"-f" (us/-main "-f" "dailyX")))
  (is (thrown-with-msg? IllegalArgumentException #"Failed to validate \"-f" (us/-main "-f" "dailyX"))))

(deftest user-summary-launcher-parse-args-with-except-users
  (is (= ["2015-04-15T00:00:00.000Z" "2015-04-16T00:00:00.000Z" :daily ["bob" "joe"] [:user :cloud]]
         (us/parse-args ["-f" "daily" "-d" "2015-04-15" "-e" "bob,joe"]))))

(deftest user-summary-launcher-parse-args-without-date
  (time/do-at
    (time/date-time 2015 4 16 8 32)
    (is (= ["2015-04-15T00:00:00.000Z" "2015-04-16T00:00:00.000Z" :daily [] [:user :cloud]]
           (us/parse-args ["-f" "daily"])))
    (is (= ["2015-04-06T00:00:00.000Z" "2015-04-13T00:00:00.000Z" :weekly [] [:user :cloud]]
           (us/parse-args ["-f" "weekly"])))
    (is (= ["2015-03-01T00:00:00.000Z" "2015-04-01T00:00:00.000Z" :monthly [] [:user :cloud]]
           (us/parse-args ["-f" "monthly"])))))

(deftest user-summary-launcher-parse-args-with-date
  (is (= ["2015-04-16T00:00:00.000Z" "2015-04-17T00:00:00.000Z" :daily [] [:user :cloud]]
         (us/parse-args ["-f" "daily" "-d" "2015-04-16"])))
  (is (= ["2015-04-16T00:00:00.000Z" "2015-04-23T00:00:00.000Z" :weekly [] [:user :cloud]]
         (us/parse-args ["-f" "weekly" "-d" "2015-04-16"])))
  (is (= ["2015-04-16T00:00:00.000Z" "2015-05-16T00:00:00.000Z" :monthly [] [:user :cloud]]
         (us/parse-args ["-f" "monthly" "-d" "2015-04-16"]))))

(deftest cloud-summary-launcher
  (time/do-at
    (time/date-time 2015 12 1 3 14)
  (is (= ["2015-11-01T00:00:00.000Z" "2015-12-01T00:00:00.000Z" :monthly ["test1" "test2"] [:cloud]]
         (us/parse-args ["-f" "monthly" "-e" "test1, test2" "-g" "cloud"])))))