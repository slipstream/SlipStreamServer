(ns com.sixsq.slipstream.db.es.aggregate-test
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.es.aggregate :as t]
    [com.sixsq.slipstream.db.es.utils :as eu])
  (:import (org.elasticsearch.search.sort SortOrder)
           (org.elasticsearch.action.search SearchRequestBuilder)
           (org.elasticsearch.search.aggregations AggregationBuilder)))

(deftest check-aggregator-constructors
  (doseq [algo (keys t/aggregator-constructors)]
    (let [algo-fn (t/aggregator-constructors algo)]
      (is (instance? AggregationBuilder (algo-fn "field-name"))))))

(deftest check-aggregation-in-search
  (eu/with-es-test-client
    (let [n 10
          type "test-resource"
          shuffled (shuffle
                     (doall
                       (for [n (range n)]
                         [(eu/random-index-name)
                          (eu/edn->json {:_acl-users ["admin"], :number n, :doubled (* 2 n)})])))]

      ;; insert generated records in random order
      (is (not (.hasFailures (eu/bulk-create client index type shuffled))))

      (let [result (eu/search client index type {:user-name   "admin"
                                                 :cimi-params {:first  1
                                                               :last   (* 2 n)
                                                               :metric {:min ["number" "doubled"]
                                                                        :max ["number" "doubled"]
                                                                        :sum ["number" "doubled"]
                                                                        :avg ["number" "doubled"]}}})

            total (-> result :hits :total)
            aggregations (:aggregations result)]

        (is (= n total))

        (is (= {:min:number  {:value 0.0}
                :max:number  {:value (double (dec n))}
                :sum:number  {:value (double (apply + (range n)))}
                :avg:number  {:value (/ (double (apply + (range n))) n)}

                :min:doubled {:value 0.0}
                :max:doubled {:value (double (* 2 (dec n)))}
                :sum:doubled {:value (double (* 2 (apply + (range n))))}
                :avg:doubled {:value (/ (double (* 2 (apply + (range n)))) n)}}
               aggregations))))))
