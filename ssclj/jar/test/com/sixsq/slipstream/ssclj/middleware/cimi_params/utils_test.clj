(ns com.sixsq.slipstream.ssclj.middleware.cimi-params.utils-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params.utils :as t]))

(deftest check-as-vector
  (are [expect arg] (= expect (t/as-vector arg))
                    [] nil
                    [1] 1
                    ["a"] "a"
                    [1 2] '(1 2)
                    [1 2] [1 2]
                    [1 "a"] '(1 "a")))

(deftest check-as-long
  (are [expect arg] (= expect (t/as-long arg))
                    nil [1]
                    nil {:a 1}
                    nil #{1}
                    1 1
                    -1 -1
                    nil 2.5
                    nil 10/3
                    1 "1"
                    -1 "-1"
                    nil "2.5"
                    nil "10/3"))

(deftest check-first-valid-long
  (are [expect arg] (= expect (t/first-valid-long arg))
                    nil []
                    nil ["a"]
                    nil ["a" "b"]
                    1 [1]
                    1 ["1"]
                    1 ["a" 1]
                    1 ["a" 1 2]
                    1 [1 2]
                    1 ["a" 1 "c"]
                    -1 [-1]
                    -1 ["a" "-1" "c"]
                    nil [nil nil]
                    nil [{:a 1} [2]]))

(deftest check-get-index
  (are [expect arg] (= expect (t/get-index arg "k"))
                    nil {"k" nil}
                    nil {}
                    1 {"k" "1"}
                    1 {"k" 1}
                    1 {"k" ["a" 1]}
                    1 {"k" ["1" "2"]}))

(deftest check-comma-split
  (are [expect arg] (= expect (t/comma-split arg))
                    ["a" "b"] "a,b"
                    ["a" "b"] " a , b "
                    ["a" "b"] ", a , b ,"
                    [] ""
                    [] ","
                    [] nil))

(deftest check-reduce-select-set
  (are [expect arg] (= expect (t/reduce-select-set arg))
                    #{"a" "b"} (set ["a" "b" "a" "b"])
                    nil (set ["a" "b" "*"])
                    nil nil))

(deftest check-reduce-expand-set
  (are [expect arg] (= expect (t/reduce-expand-set arg))
                    :all #{"*"}
                    :none #{}
                    :none nil
                    #{"a" "b"} #{"a" "b"}))

(deftest check-orderby-clause
  (are [expect arg] (= expect (t/orderby-clause arg))
                    [":" :asc] ":"
                    [":a" :desc] ":a:desc"
                    ["a" :asc] "a"
                    ["a:" :asc] "a:"
                    ["a" :desc] "a:desc"
                    ["a:dummy" :asc] "a:dummy"))

(deftest check-metric-clause
  (are [expect arg] (= expect (t/metric-clause arg))
                    nil ""
                    nil "attr-name"
                    nil "attr-name:"
                    nil "attr-name: "
                    nil ":algo"
                    nil " :algo"
                    nil "attr-name:BAD"
                    [:sum "attr"] "attr:sum"
                    [:sum "attr"] " attr:sum"
                    [:sum "attr:name"] "attr:name:sum"))

(deftest check-update-metric-map
  (are [expect initial] (t/update-metric-map initial (t/metric-clause "attr1:sum"))
                        {:sum ["attr1"]} {}
                        {:sum ["attr0" "attr1"]} {:sum ["attr0"]}
                        {:min ["attr0"], :sum ["attr0" "attr1"]} {:min ["attr0"], :sum ["attr0"]}))
