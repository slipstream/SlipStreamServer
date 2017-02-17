(ns com.sixsq.slipstream.db.es.es-util-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.es.es-util :as eu]))

(deftest test-create-test-es-client
  (is (not (= nil (eu/create-test-es-client)))))




