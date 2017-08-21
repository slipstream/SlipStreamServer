(ns com.sixsq.slipstream.db.es.select-test
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.es.select :as t]
    [com.sixsq.slipstream.db.es.utils :as eu])
  (:import (org.elasticsearch.search.sort SortOrder)
           (org.elasticsearch.action.search SearchRequestBuilder)
           (org.elasticsearch.search.aggregations AggregationBuilder)))

(deftest check-select-keys
  (eu/with-es-test-client
    (let [n 10
          type "test-resource"
          shuffled (shuffle
                     (doall
                       (for [n (range n)]
                         [(eu/random-index-name)
                          (eu/edn->json {:_acl-users ["admin"]
                                         :resourceURI "resource-uri"
                                         :number n
                                         :doubled (* 2 n)
                                         :x1 "x1"
                                         :x2 "x2"
                                         :acl "acl"})])))]

      ;; insert generated records in random order
      (is (not (.hasFailures (eu/bulk-create client index type shuffled))))

      (let [result (eu/search client index type {:user-name   "admin"
                                                 :cimi-params {:first  1
                                                               :last   (* 2 n)
                                                               :select #{"resourceURI" "doubled" "x*"}}})

            total (-> result :hits :total)
            docs (map :_source (-> result :hits :hits))]

        (is (= n total))

        (is (every? :resourceURI docs))
        (is (every? :doubled docs))
        (is (every? :x1 docs))
        (is (every? :x2 docs))
        (is (every? :acl docs))
        (is (not-every? :number docs))))))
