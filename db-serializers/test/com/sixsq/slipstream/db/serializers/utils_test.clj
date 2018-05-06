(ns com.sixsq.slipstream.db.serializers.utils-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.dbtest.es.utils-esdb :as ud]))

(deftest test-as-boolean
  (is (= true (u/as-boolean true)))
  (is (= false (u/as-boolean false)))
  (is (= true (u/as-boolean "true")))
  (is (= false (u/as-boolean "false")))
  (is (= nil (u/as-boolean nil))))

(deftest test-start-stop-es
  (is (instance? clojure.lang.Var$Unbound ud/*es-server*))
  (ud/create-test-es-db-uncond)
  (is (not (instance? clojure.lang.Var$Unbound ud/*es-server*)))
  (ud/close-es-server!)
  (is (instance? clojure.lang.Var$Unbound ud/*es-server*)))