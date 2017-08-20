(ns com.sixsq.slipstream.db.es.order-test
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.es.order :as t]
    [com.sixsq.slipstream.db.es.utils :as eu]
    [clojure.data.json :as json])
  (:import (org.elasticsearch.search.sort SortOrder)
           (org.elasticsearch.action.search SearchRequestBuilder)
           (org.elasticsearch.search SearchHits SearchHit)))

(deftest check-direction->sort-order
  (is (= SortOrder/ASC (t/direction->sort-order :asc)))
  (is (= SortOrder/DESC (t/direction->sort-order :desc)))
  (is (thrown-with-msg? IllegalArgumentException #".*invalid.*:unknown.*" (t/direction->sort-order :unknown))))

(defn test-search-results [client index type n order test-fn]
  (let [search-response (eu/search client index type {:user-name   "admin"
                                                      :cimi-params {:first   1
                                                                    :last    (* 2 n)
                                                                    :orderby [["number" order]]}})
        status (.getStatus (.status search-response))
        ^SearchHits search-hits (.getHits search-response)
        nhits (.totalHits search-hits)
        hits (.hits search-hits)
        docs (map (fn [^SearchHit h] (eu/json->edn (.sourceAsString h))) hits)]

    (is (= 200 status))
    (is (= n nhits))
    (is (= n (count hits)))
    (is (= n (count docs)))
    (is (apply test-fn (map :number docs)))))

(deftest check-sorting-in-search
  (eu/with-es-test-client
    (let [n 10
          type "test-resource"
          shuffled (shuffle
                     (doall
                       (for [n (range n)]
                         [(eu/random-index-name)
                          (eu/edn->json {:_acl-users ["admin"], :number n})])))]

      ;; insert generated records in random order
      (is (not (.hasFailures (eu/bulk-create client index type shuffled))))

      ;; check that searches with ordering both work correctly
      (test-search-results client index type n :asc <)
      (test-search-results client index type n :desc >))))
