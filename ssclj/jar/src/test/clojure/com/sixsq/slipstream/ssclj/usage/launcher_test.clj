(ns com.sixsq.slipstream.ssclj.usage.launcher-test
  (:require 
    [com.sixsq.slipstream.ssclj.usage.launcher :refer :all]
    [clojure.tools.cli :refer [parse-opts]]    
    [clojure.test :refer :all]))

(defn success-matches?   
  [[expected-start expected-end] [actual-code [actual-start actual-end]]]
  (is (= :success actual-code))
  (is (= expected-start actual-start))
  (is (= expected-end actual-end)))

(defn error-matches?   
  [[expected-code expected-msg] [actual-code actual-msg]]
  (is (= expected-code actual-code))
  (is (.startsWith actual-msg expected-msg))) ;; TODO with a regexp would be better

(deftest test-launcher-checks-args
  (is (success-matches? ["2015-01-15T00:00:00.0Z" "2015-01-16T00:00:00.0Z"] 
    (analyze-args ["-s" "2015-01-15T00:00:00.0Z" "-e" "2015-01-16T00:00:00.0Z"])))

   (is (error-matches? [:help  "Triggers"] (analyze-args ["-h"])))
   (is (error-matches? [:help  "Triggers"] (analyze-args ["--help"])))
   (is (error-matches? [:error "The following errors"] (analyze-args ["-p" "123"])))
   (is (error-matches? [:error "The following errors"] (analyze-args ["-p" "123" "-s" "2015-01-15T00:00:00.0Z"]))))
