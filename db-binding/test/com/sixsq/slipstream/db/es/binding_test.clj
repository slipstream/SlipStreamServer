(ns com.sixsq.slipstream.db.es.binding-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.db.es.binding :as t]
    [com.sixsq.slipstream.db.es.es-node :as es-node]
    [com.sixsq.slipstream.db.binding-lifecycle :as lifecycle])
  (:import
    (clojure.lang Var$Unbound)
    (java.io StringWriter)
    (org.elasticsearch.node Node)))


(deftest test-set-close-es-client
  (is (instance? Var$Unbound t/*client*))
  (t/set-client! (StringWriter.))
  (is (not (instance? Var$Unbound t/*client*)))
  (t/close-client!)
  (is (instance? Var$Unbound t/*client*)))


(deftest check-native-protocol
  (with-open [test-node (es-node/create-test-node)
              client (-> test-node
                         (.client)
                         t/wait-client-create-index)]
    (-> client
        t/->ESBindingLocal
        lifecycle/check-binding-lifecycle)))
