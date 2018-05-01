(ns com.sixsq.slipstream.db.es-rest.binding-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.binding-lifecycle :as lifecycle]
    [com.sixsq.slipstream.db.binding-queries :as queries]
    [com.sixsq.slipstream.db.es.es-node :as es-node]
    [com.sixsq.slipstream.db.es-rest.binding :as t]
    [com.sixsq.slipstream.db.es-rest.test-utils :as test-utils]))


(deftest check-es-rest-protocol

  (with-open [test-node (es-node/create-test-node)
              client (t/create-client {:hosts ["localhost:9200"]})]
    (test-utils/initialize-db client)
    (-> client
        t/->ElasticsearchRestBinding
        lifecycle/check-binding-lifecycle))


  (with-open [test-node (es-node/create-test-node)
              client (t/create-client {:hosts ["localhost:9200"]})]
    (test-utils/initialize-db client)
    (-> client
        t/->ElasticsearchRestBinding
        queries/check-binding-queries)))
