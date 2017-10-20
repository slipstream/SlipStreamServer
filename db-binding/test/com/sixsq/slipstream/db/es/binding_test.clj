(ns com.sixsq.slipstream.db.es.binding-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.es.binding :as eb]))

(deftest test-set-close-es-client
  (is (instance? clojure.lang.Var$Unbound eb/*client*))
  (eb/set-client! (java.io.StringWriter.))
  (is (not (instance? clojure.lang.Var$Unbound eb/*client*)))
  (eb/close-client!)
  (is (instance? clojure.lang.Var$Unbound eb/*client*)))
