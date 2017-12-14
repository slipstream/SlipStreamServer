(ns com.sixsq.slipstream.db.es.utils-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.dbtest.es.utils :as eut]
    [com.sixsq.slipstream.db.es.utils :as eu])
  (:import (org.elasticsearch.action.admin.indices.create CreateIndexResponse)))

(deftest test-with-es-test-client
  (eut/with-es-test-client
    (is client)
    (is node)
    (is index)
    (is (eu/index-exists? client index))))

(deftest test-create-index-and-index-exists?
  (eut/with-es-test-client
    (let [index-name                    (eu/random-index-name)
          ^CreateIndexResponse response (eu/create-index client index-name)]
      (is (.isAcknowledged response))
      (is (eu/index-exists? client index-name))
      (is (not (eu/index-exists? client "WrongIndex"))))))

(deftest test-refresh-indices
  (eut/with-es-test-client
    (is client)
    (is index)
    (is (= 1 (count (eu/get-all-indices client))))
    (eu/refresh-index client index)
    (eu/refresh-indices client [index])
    (eu/refresh-all-indices client)))