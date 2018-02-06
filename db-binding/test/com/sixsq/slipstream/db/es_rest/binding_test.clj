(ns com.sixsq.slipstream.db.es-rest.binding-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.binding-lifecycle :as lifecycle]
    [com.sixsq.slipstream.dbtest.es.utils :as dbtest]
    [com.sixsq.slipstream.db.es-rest.binding :as t]))


(deftest check-spandex
  (with-open [test-node (dbtest/create-test-node)]
    (-> {:hosts ["localhost:9200"]}
        t/create-client
        t/->ElasticsearchRestBinding
        lifecycle/check-binding-lifecycle)))
