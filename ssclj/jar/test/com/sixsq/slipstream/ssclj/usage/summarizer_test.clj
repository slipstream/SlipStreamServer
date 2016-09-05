(ns com.sixsq.slipstream.ssclj.usage.summarizer-test
  (:require
    [com.sixsq.slipstream.ssclj.usage.summarizer-imp :as us]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [clj-time.core :as time]
    [clojure.test :refer :all]))

(use-fixtures :each ltu/with-test-client-fixture)

(deftest user-summary-without-date
  (ltu/with-test-client
    (us/do-summarize "-f" "daily")
    (us/do-summarize "-f" "weekly")
    (us/do-summarize "-f" "monthly")))

(deftest user-summary-with-args
  (ltu/with-test-client
    (us/do-summarize "-d" "2015-01-01" "-f" "daily")
    (us/do-summarize "--date" "2015-01-01" "--frequency" "daily")))

(deftest user-summary-requires-frequency
  (is (thrown? IllegalArgumentException (us/do-summarize))))

(deftest user-summary-checks-args
  (is (thrown-with-msg? IllegalArgumentException #"Unknown option" (us/do-summarize "-x" "2015-01-01")))
  (is (thrown-with-msg? IllegalArgumentException #"Failed to validate \"-d" (us/do-summarize "-d" "2015-01")))
  (is (thrown-with-msg? IllegalArgumentException #"Failed to validate \"-f" (us/do-summarize "-f" "dailyX")))
  (is (thrown-with-msg? IllegalArgumentException #"Failed to validate \"-f" (us/do-summarize "-f" "dailyX"))))

(deftest user-summary-launcher-parse-args-with-except-users
  (is (= ["2015-04-15T00:00:00.000Z" :daily ["bob" "joe"] [:user :cloud] 1]
         (us/parse-args ["-f" "daily" "-d" "2015-04-15" "-e" "bob,joe"]))))

(deftest user-summary-launcher-parse-args-without-date
  (time/do-at
    (time/date-time 2015 4 16 8 32)
    (is (= ["2015-04-15T00:00:00.000Z" :daily [] [:user :cloud] 1]
           (us/parse-args ["-f" "daily"])))
    (is (= ["2015-04-06T00:00:00.000Z" :weekly [] [:user :cloud] 1]
           (us/parse-args ["-f" "weekly"])))
    (is (= ["2015-03-01T00:00:00.000Z" :monthly [] [:user :cloud] 1]
           (us/parse-args ["-f" "monthly"])))))

(deftest user-summary-launcher-parse-args-with-date
  (is (= ["2015-04-16T00:00:00.000Z" :daily [] [:user :cloud] 1]
         (us/parse-args ["-f" "daily" "-d" "2015-04-16"])))

  (is (= ["2015-04-16T00:00:00.000Z" :weekly [] [:user :cloud] 1]
         (us/parse-args ["-f" "weekly" "-d" "2015-04-16"])))
  (is (= ["2015-04-16T00:00:00.000Z" :weekly [] [:user :cloud] 5]
         (us/parse-args ["-f" "weekly" "-d" "2015-04-16" "-n" "5"])))

  (is (= ["2015-04-16T00:00:00.000Z" :monthly [] [:user :cloud] 1]
         (us/parse-args ["-f" "monthly" "-d" "2015-04-16"]))))

(deftest cloud-summary-launcher
  (time/do-at
    (time/date-time 2015 12 1 3 14)
    (is (= ["2015-11-01T00:00:00.000Z" :monthly ["test1" "test2"] [:cloud] 1]
           (us/parse-args ["-f" "monthly" "-e" "test1, test2" "-g" "cloud"])))))

(deftest test-backward
  (is (= [["2015-10-30T00:00:00.000Z" "2015-10-31T00:00:00.000Z"]
          ["2015-10-31T00:00:00.000Z" "2015-11-01T00:00:00.000Z"]
          ["2015-11-01T00:00:00.000Z" "2015-11-02T00:00:00.000Z"]]
         (us/backward-periods "2015-11-01T00:00:00.000Z" 3 :daily)))
  (is (= [["2015-10-25T00:00:00.000Z" "2015-11-01T00:00:00.000Z"]
          ["2015-11-01T00:00:00.000Z" "2015-11-08T00:00:00.000Z"]]
         (us/backward-periods "2015-11-01T00:00:00.000Z" 2 :weekly)))
  (is (= [["2015-10-01T00:00:00.000Z" "2015-11-01T00:00:00.000Z"]
          ["2015-11-01T00:00:00.000Z" "2015-12-01T00:00:00.000Z"]]
         (us/backward-periods "2015-11-01T00:00:00.000Z" 2 :monthly))))
