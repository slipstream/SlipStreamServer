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
(expect (passes-fn :DoubleQuoteString)
        (from-each [s ["\"\""
                       "\"a\""
                       "\"a1\""
                       "\"\\\"a\""
                       "\"a\\\"\""
                       "\"a\\\"a\""
                       "\"b0\""
                       "\"b-0\""]]
                   s))

;; valid single quoted strings
(expect (passes-fn :SingleQuoteString)
        (from-each [s ["''"
                       "'a'"
                       "'a1'"
                       "'\\'a'"
                       "'a\\''"
                       "'a\\'a'"
                       "'b0'"
                       "'b-0'"]]
                   s))

;; valid dates
(expect (passes-fn :DateValue)
        (from-each [s ["2012-01"
                       "2012-01-02"
                       "2012-01-02T13:14:25Z"
                       "2012-01-02T13:14:25.6Z"
                       "2012-01-02T13:14:25-01:15"
                       "2012-01-02T13:14:25.6-01:15"
                       "2012-01-02T13:14:25+02:30"
                       "2012-01-02T13:14:25.6+02:30"]]
                   s))

;; invalid dates
(expect (fails-fn :DateValue)
        (from-each [s ["2012"
                       "2012-01-99T13:14:25.6ZZ"
                       "2012-01-02T13:14:25.6Q"
                       "2012-01:02T25:14:25.6-01:15"
                       "2012-01-02T13:14:25.6+02-30"]]
                   s))

;; valid filters
(expect (passes-fn :Filter)
        (from-each [s ["alpha=3"
                       "alpha!=3"
                       "3=alpha"
                       "alpha=3 and beta=4"
                       "3=alpha and 4=beta"
                       "(alpha=3)"
                       "(3=alpha)"
                       "property['beta']='4'"
                       "property['beta']!='4'"
                       "property['beta']<'4'"
                       "alpha=3 and beta=4"
                       "alpha=3 or beta=4"
                       "alpha=3 and beta=4 or gamma=5 and delta=6"
                       "alpha=3 and (beta=4 or gamma=5) and delta=6"
                       "b='b0'"
                       "b='b-0'"
                       "'b0'=b"
                       "'b-0'=b"
                       "cloud-vm-instanceid='exo:123-456'"]]
                   s))

;; invalid filters
(expect (fails-fn :Filter)
        (from-each [s [""
                       "()"
                       "alpha=beta"
                       "alpha=3.2"
                       "alpha&&4"
                       "property[beta]='4'"
                       "property['beta']=4"
                       "4=property['beta']"]]
                   s))

;; valid attributes
(expect (passes-fn :Attribute)
        (from-each [s ["a"
                       "alpha"
                       "alpha123"
                       "a1"
                       "a1/b2"
                       "a1/b2/c3"
                       "schema-org:attr1"
                       "schema-org:attr1/schema-org:attr2"]]
                   s))

;; invalid attributes
(expect (fails-fn :Attribute)
        (from-each [s [""
                       "_"
                       "-"
                       "/"
                       "a-"
                       "1"
                       "1a"
                       "_a"
                       "_1"
                       "a1/"
                       "/a1"
                       "a/1"
                       ":a"
                       "a:"
                       "schema-org:a:b"
                       "schema-org:a/"]]
                   s))

(run-all-tests)
