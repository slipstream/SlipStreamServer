(ns com.sixsq.slipstream.ssclj.middleware.cimi-params-impl-test
  (:require
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]
    [com.sixsq.slipstream.ssclj.middleware.cimi-params-impl :refer :all]
    [expectations :refer :all]))

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

(expect {:cimi-params {:k "value"}} (add-cimi-param {} :k "value"))
(expect {:cimi-params {:a 1, :k "value"}} (add-cimi-param {:cimi-params {:a 1}} :k "value"))

(expect [] (as-vector nil))
(expect [1] (as-vector 1))
(expect ["a"] (as-vector "a"))
(expect [1 2] (as-vector '(1 2)))
(expect [1 2] (as-vector [1 2]))
(expect [1 "a"] (as-vector '(1 "a")))

(expect nil (as-long [1]))
(expect nil (as-long {:a 1}))
(expect nil (as-long #{1}))
(expect 1 (as-long 1))
(expect -1 (as-long -1))
(expect nil (as-long 2.5))
(expect nil (as-long 10/3))
(expect 1 (as-long "1"))
(expect -1 (as-long "-1"))
(expect nil (as-long "2.5"))
(expect nil (as-long "10/3"))

(expect nil (first-valid-long []))
(expect nil (first-valid-long ["a"]))
(expect nil (first-valid-long ["a" "b"]))
(expect 1 (first-valid-long [1]))
(expect 1 (first-valid-long ["1"]))
(expect 1 (first-valid-long ["a" 1]))
(expect 1 (first-valid-long ["a" 1 2]))
(expect 1 (first-valid-long [1 2]))
(expect 1 (first-valid-long ["a" 1 "c"]))
(expect -1 (first-valid-long [-1]))
(expect -1 (first-valid-long ["a" "-1" "c"]))
(expect nil (first-valid-long [nil nil]))
(expect nil (first-valid-long [{:a 1} [2]]))

(expect nil (get-index {"k" nil} "k"))
(expect nil (get-index {} "k"))
(expect 1 (get-index {"k" "1"} "k"))
(expect 1 (get-index {"k" 1} "k"))
(expect 1 (get-index {"k" ["a" 1]} "k"))
(expect 1 (get-index {"k" ["1" "2"]} "k"))

(expect {:cimi-params {:first nil :last nil} :params {}} (process-first-last {:params {}}))

(expect {:first 1 :last 2} (:cimi-params (process-first-last {:params {"$first" 1, "$last" 2}})))
(expect {:first 1 :last 2} (:cimi-params (process-first-last {:params {"$first" "1", "$last" "2"}})))
(expect {:first 1 :last 2} (:cimi-params (process-first-last {:params {"$first" ["a" "1"], "$last" ["b" "2"]}})))
(expect {:first nil :last nil} (:cimi-params (process-first-last {:params {"$first" ["a"], "$last" nil}})))
(expect {:first nil :last nil} (:cimi-params (process-first-last {:params {}})))

(expect "application/json" (set-and-extract process-format "format" "json"))
(expect "application/json" (set-and-extract process-format "format" "JSON"))
(expect "application/json" (set-and-extract process-format "format" " JSON "))
(expect "application/xml" (set-and-extract process-format "format" "xml"))
(expect "application/xml" (set-and-extract process-format "format" "XML"))
(expect "application/xml" (set-and-extract process-format "format" " XML "))
(expect "application/edn" (set-and-extract process-format "format" "edn"))
(expect "application/edn" (set-and-extract process-format "format" "EDN"))
(expect "application/edn" (set-and-extract process-format "format" " EDN "))

(expect nil (set-and-extract process-format "format" "unknown"))
(expect nil (set-and-extract process-format "format" nil))

(expect "application/json" (set-and-extract process-format "format" ["json" "xml"]))
(expect "application/json" (set-and-extract process-format "format" ["unknown" "json" "xml"]))

(expect ["a" "b"] (comma-split "a,b"))
(expect ["a" "b"] (comma-split " a , b "))
(expect ["a" "b"] (comma-split ", a , b ,"))
(expect [] (comma-split ""))
(expect [] (comma-split ","))
(expect [] (comma-split nil))

(expect #{"a" "b"} (reduce-select-set (set ["a" "b" "a" "b"])))
(expect nil (reduce-select-set (set ["a" "b" "*"])))
(expect nil (reduce-select-set nil))

(expect nil (set-and-extract process-select "select" nil))
(expect nil (set-and-extract process-select "select" "*"))
(expect #{"a" "resourceURI"} (set-and-extract process-select "select" "a"))
(expect #{"a" "resourceURI"} (set-and-extract process-select "select" " a "))
(expect #{"a" "resourceURI"} (set-and-extract process-select "select" "a,a"))
(expect #{"a" "resourceURI"} (set-and-extract process-select "select" [" a,a" "a" "a"]))
(expect #{"a" "a2" "resourceURI"} (set-and-extract process-select "select" " a, a2 "))

(expect :all (reduce-expand-set #{"*"}))
(expect :none (reduce-expand-set #{}))
(expect :none (reduce-expand-set nil))
(expect #{"a" "b"} (reduce-expand-set #{"a" "b"}))

(expect :none (set-and-extract process-expand "expand" nil))
(expect :all (set-and-extract process-expand "expand" "*"))
(expect #{"a"} (set-and-extract process-expand "expand" "a"))
(expect #{"a" "b"} (set-and-extract process-expand "expand" "a,b"))
(expect #{"a" "b"} (set-and-extract process-expand "expand" " a , b "))
(expect #{"a" "b"} (set-and-extract process-expand "expand" ["a" "b"]))

(expect nil (orderby-clause ":"))
(expect nil (orderby-clause ":a:desc"))
(expect ["a" :asc] (orderby-clause "a"))
(expect ["a" :asc] (orderby-clause "a:"))
(expect ["a" :desc] (orderby-clause "a:desc"))
(expect ["a" :asc] (orderby-clause "a:dummy"))

(expect [] (set-and-extract process-orderby "orderby" nil))
(expect [["a" :asc]] (set-and-extract process-orderby "orderby" "a:asc"))
(expect [["a" :desc]] (set-and-extract process-orderby "orderby" "a:desc"))
(expect [["a" :desc] ["b" :asc]] (set-and-extract process-orderby "orderby" "a:desc,b"))
(expect [["a" :desc] ["b" :desc]] (set-and-extract process-orderby "orderby" ["a:desc" "b:desc"]))
(expect [["a" :desc]] (set-and-extract process-orderby "orderby" ["a:desc" ":b"]))
(expect [["a" :desc] ["b" :desc]] (set-and-extract process-orderby "orderby" [" a : desc " "b : desc"]))

(expect [:Filter
         [:AndExpr
          [:Comp
           [:Filter
            [:AndExpr
             [:Comp [:Attribute "a"] [:Op "="] [:IntValue "1"]]]]]]]
        (set-and-extract process-filter "filter" "a=1"))

(expect [:Filter
         [:AndExpr
          [:Comp
           [:Filter
            [:AndExpr
             [:Comp [:Attribute "a"] [:Op "="] [:IntValue "1"]]]]]]]
        (set-and-extract process-filter "filter" ["a=1"]))

(expect [:Filter
         [:AndExpr
          [:Comp
           [:Filter
            [:AndExpr
             [:Comp [:Attribute "a"] [:Op "="] [:IntValue "1"]]]]]
          [:AndExpr
           [:Comp
            [:Filter
             [:AndExpr
              [:Comp [:Attribute "b"] [:Op "="] [:IntValue "2"]]]]]]]]
        (set-and-extract process-filter "filter" ["a=1" "b=2"]))

(expect (set-and-extract process-filter "filter" "a=1")
        (set-and-extract process-filter "filter" ["a=1"]))

(expect (parser/parse-cimi-filter "(a=1) and (b=2)")
        (set-and-extract process-filter "filter" ["a=1" "b=2"]))

(expect (parser/parse-cimi-filter "(a=1) and (b=2) and (c=3)")
        (set-and-extract process-filter "filter" ["a=1" "b=2" "c=3"]))

(expect (parser/parse-cimi-filter "(a=1 or c=3) and (b=2)")
        (set-and-extract process-filter "filter" ["a=1 or c=3" "b=2"]))


