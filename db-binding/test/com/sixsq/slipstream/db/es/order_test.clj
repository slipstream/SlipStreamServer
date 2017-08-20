(ns com.sixsq.slipstream.db.es.order-test
  (:refer-clojure :exclude [read update])
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.es.order :as t]
    [com.sixsq.slipstream.db.es.utils :as eu])
  (:import (org.elasticsearch.search.sort SortOrder)
           (org.elasticsearch.action.search SearchType SearchRequestBuilder)))

(deftest check-direction->sort-order
  (is (= SortOrder/ASC (t/direction->sort-order :asc)))
  (is (= SortOrder/DESC (t/direction->sort-order :desc)))
  (is (thrown-with-msg? IllegalArgumentException #".*invalid.*:unknown.*" (t/direction->sort-order :unknown))))

(deftest check-add-sorter
  (eu/with-es-test-client
    (let [^SearchRequestBuilder request (.. client
                                            (prepareSearch (into-array String [index]))
                                            (setSearchType SearchType/DEFAULT))]
      (is request)
      (is (instance? SearchRequestBuilder request))

      (t/add-sorter request ["field1" :asc])

      ;; FIXME: FINISH THIS.
      )))
