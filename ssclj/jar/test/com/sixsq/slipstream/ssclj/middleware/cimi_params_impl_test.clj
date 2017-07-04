(ns com.sixsq.slipstream.ssclj.middleware.cimi-params-impl-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params-impl :as t]))

(defn set-and-extract
  "Sets the $key-name value in the parameters and then extracts
   the value of :key-name from :cimi-params in the result."
  [f key-name v]
  (let [kw (keyword key-name)
        pname (str "$" key-name)]
    (->> {:params {pname v}}
         (f)
         (:cimi-params)
         (kw))))

(deftest check-add-cimi-params
  (are [expect args] (= expect (apply t/add-cimi-param args))
                     {:cimi-params {:k "value"}} [{} :k "value"]
                     {:cimi-params {:a 1, :k "value"}} [{:cimi-params {:a 1}} :k "value"]))

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

(deftest check-process-first-last
  (let [last-other-than-default (* 2 t/default-last)]
    (are [expect arg] (= expect (:cimi-params (t/process-first-last arg)))
                      {:first nil :last t/default-last} {:params {}}
                      {:first nil :last t/default-last} {:params {}}
                      {:first nil :last t/default-last} {:params {"$first" ["a"], "$last" nil}}
                      {:first nil :last last-other-than-default} {:params {"$last" last-other-than-default}} ;; something other than default
                      {:first 2 :last (inc t/default-last)} {:params {"$first" 2}} ;; adjust last value
                      {:first 1 :last 2} {:params {"$first" 1, "$last" 2}}
                      {:first 1 :last 2} {:params {"$first" "1", "$last" "2"}}
                      {:first 1 :last 2} {:params {"$first" ["a" "1"], "$last" ["b" "2"]}})))

(deftest check-set-and-extract
  (are [expect arg] (= expect (set-and-extract t/process-format "format" arg))
                    "application/json" "json"
                    "application/json" "JSON"
                    "application/json" " JSON "
                    "application/xml" "xml"
                    "application/xml" "XML"
                    "application/xml" " XML "
                    "application/edn" "edn"
                    "application/edn" "EDN"
                    "application/edn" " EDN "
                    nil "unknown"
                    nil nil
                    "application/json" ["json" "xml"]
                    "application/json" ["unknown" "json" "xml"]))

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

(deftest check-select-param
  (are [expect arg] (= expect (set-and-extract t/process-select "select" arg))
                    nil nil
                    nil "*"
                    #{"a" "resourceURI"} "a"
                    #{"a" "resourceURI"} " a "
                    #{"a" "resourceURI"} "a,a"
                    #{"a" "resourceURI"} [" a,a" "a" "a"]
                    #{"a" "a2" "resourceURI"} " a, a2 "))

(deftest check-reduce-expand-set
  (are [expect arg] (= expect (t/reduce-expand-set arg))
                    :all #{"*"}
                    :none #{}
                    :none nil
                    #{"a" "b"} #{"a" "b"}))

(deftest check-reduce-param
  (are [expect arg] (= expect (set-and-extract t/process-expand "expand" arg))
                    :none nil
                    :all "*"
                    #{"a"} "a"
                    #{"a" "b"} "a,b"
                    #{"a" "b"} " a , b "
                    #{"a" "b"} ["a" "b"]))

(deftest check-orderby-clause
  (are [expect arg] (= expect (t/orderby-clause arg))
                    [":" :asc] ":"
                    [":a" :desc] ":a:desc"
                    ["a" :asc] "a"
                    ["a:" :asc] "a:"
                    ["a" :desc] "a:desc"
                    ["a:dummy" :asc] "a:dummy"))

(deftest check-orderby-param
  (are [expect arg] (= expect (set-and-extract t/process-orderby "orderby" arg))
                    [] nil
                    [["a" :asc]] "a:asc"
                    [["a" :desc]] "a:desc"
                    [["a" :desc] ["b" :asc]] "a:desc,b"
                    [["a" :desc] ["b" :desc]] ["a:desc" "b:desc"]
                    [["a" :desc] [":b" :asc]] ["a:desc" ":b"]
                    [["a" :desc] ["b" :desc]] [" a :desc " "b :desc"]))

(deftest check-filter
  (are [arg1 arg2] (= (parser/parse-cimi-filter arg1) (set-and-extract t/process-filter "filter" arg2))
                   "(a=1)" "a=1"
                   "(a=1)" ["a=1"]
                   "(a=1) and (b=2)" ["a=1" "b=2"]
                   "(a=1) and (b=2) and (c=3)" ["a=1" "b=2" "c=3"]
                   "(a=1 or c=3) and (b=2)" ["a=1 or c=3" "b=2"]))
