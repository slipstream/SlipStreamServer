(ns slipstream.credcache.renewal-test
  (:require
    [expectations :refer :all]
    [slipstream.credcache.renewal :refer :all]))

;; default renew implementation
(expect nil?
        (from-each [m [{}
                       {:typeURI "unknown"}
                       {:typeURI "unknown" :other "field"}]]
                   (renew m)))
