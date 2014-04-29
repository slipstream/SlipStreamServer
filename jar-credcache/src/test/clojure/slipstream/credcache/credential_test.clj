(ns slipstream.credcache.credential-test
  (:require
    [expectations :refer :all]
    [slipstream.credcache.credential :refer :all]))

;; default renew implementation
(expect nil?
        (from-each [m [{}
                       {:typeURI "unknown"}
                       {:typeURI "unknown" :other "field"}]]
                   (renew m)))
