(ns com.sixsq.slipstream.ssclj.filter.parser-test
  (:require
    [com.sixsq.slipstream.ssclj.filter.parser :refer :all]
    [instaparse.core :as insta]
    [expectations :refer :all]))

(defn fails-fn
  "Provides a function that will parse a string from the given point
  in the grammar and returns a truthy value if the parsing failed."
  [start]
  (let [parser (insta/parser filter-grammar-url :start start)]
    (fn [s]
      (insta/failure? (parser s)))))

(defn passes-fn
  [start]
  (let [fails (fails-fn start)]
    (fn [s]
      (not (fails s)))))

;; valid double quoted strings
(let [passes (passes-fn :DoubleQuoteString)]
  (expect passes "\"\"")
  (expect passes "\"a\"")
  (expect passes "\"a1\"")
  (expect passes "\"\\\"a\"")
  (expect passes "\"a\\\"\"")
  (expect passes "\"a\\\"a\"")
  (expect passes "\"b0\"")
  (expect passes "\"b-0\""))

;; valid single quoted strings
(let [passes (passes-fn :SingleQuoteString)]
  (expect passes "''")
  (expect passes "'a'")
  (expect passes "'a1'")
  (expect passes "'\\'a'")
  (expect passes "'a\\''")
  (expect passes "'a\\'a'")
  (expect passes "'b0'")
  (expect passes "'b-0'"))

;; valid dates
(let [passes (passes-fn :DateValue)]
  (expect passes "2012-01")
  (expect passes "2012-01-02")
  (expect passes "2012-01-02T13:14:25Z")
  (expect passes "2012-01-02T13:14:25.6Z")
  (expect passes "2012-01-02T13:14:25-01:15")
  (expect passes "2012-01-02T13:14:25.6-01:15")
  (expect passes "2012-01-02T13:14:25+02:30")
  (expect passes "2012-01-02T13:14:25.6+02:30"))

;; invalid dates
(let [fails (fails-fn :DateValue)]
  (expect fails "2012")
  (expect fails "2012-01-99T13:14:25.6ZZ")
  (expect fails "2012-01-02T13:14:25.6Q")
  (expect fails "2012-01:02T25:14:25.6-01:15")
  (expect fails "2012-01-02T13:14:25.6+02-30"))

;; valid filters
(let [passes (passes-fn :Filter)]
  (expect passes "alpha=3")
  (expect passes "3=alpha")
  (expect passes "alpha=3 and beta=4")
  (expect passes "3=alpha and 4=beta")
  (expect passes "(alpha=3)")
  (expect passes "(3=alpha)")
  (expect passes "property['beta']='4'")
  (expect passes "property['beta']!='4'")
  (expect passes "property['beta']<'4'")                    ;; strictly this should be illegal
  (expect passes "alpha=3 and beta=4")
  (expect passes "alpha=3 or beta=4")
  (expect passes "alpha=3 and beta=4 or gamma=5 and delta=6")
  (expect passes "alpha=3 and (beta=4 or gamma=5) and delta=6")
  (expect passes "b='b0'")
  (expect passes "b='b-0'")
  (expect passes "'b0'=b")
  (expect passes "'b-0'=b")
  (expect passes "cloud-vm-instanceid='exo:123-456'"))

;; invalid filters
(let [fails (fails-fn :Filter)]
  (expect fails "")
  (expect fails "()")
  (expect fails "alpha=beta")
  (expect fails "alpha=3.2")
  (expect fails "alpha&&4")
  (expect fails "property[beta]='4'")
  (expect fails "property['beta']=4")
  (expect fails "4=property['beta']"))

;; valid attributes
(let [passes (passes-fn :Attribute)]
  (expect passes "a")
  (expect passes "alpha")
  (expect passes "alpha123")
  (expect passes "a1")
  (expect passes "_a")
  (expect passes "_1")
  (expect passes "a1/b2")                                   ;; hierarchical attributes are an extension of the spec
  (expect passes "a1/b2/c3"))

;; invalid attributes
(let [fails (fails-fn :Attribute)]
  (expect fails "")
  (expect fails "1")
  (expect fails "1a")
  (expect fails "a1/")
  (expect fails "/a1")
  (expect fails "a/1"))

(run-all-tests)