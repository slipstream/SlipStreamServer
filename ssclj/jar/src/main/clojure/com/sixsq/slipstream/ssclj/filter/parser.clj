(ns com.sixsq.slipstream.ssclj.filter.parser
  "Implements a parser for CIMI filters as defined in Section 4.1.6.1 of the 
  CIMI specification (DSP0263 v1.0.1)."
  (:require
    [clojure.java.io :as io]
    [instaparse.core :as insta]))

;; NOTE: The URL for instaparse must be a string.
(def grammar-url
  (str (io/resource "filter-grammar.txt")))

(def parser
  (insta/parser grammar-url))

