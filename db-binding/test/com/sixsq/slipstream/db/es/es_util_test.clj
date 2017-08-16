(ns com.sixsq.slipstream.db.es.es-util-test
  (:require
   [clojure.test :refer :all]
   [com.sixsq.slipstream.db.es.es-util :as eu]))

(deftest test-create-test-es-client
  (let [[client node] (eu/create-test-es-client)]
    (is (not (nil? client)))
    (is (not (nil? node)))))

(deftest test-create-test-index
  (let [[client node] (eu/create-test-es-client)
        lowercases (map char (range 97 122))
        index-name-length (rand-int 100)
        index-name (reduce str (take index-name-length (repeatedly #(rand-nth lowercases))))
        res (eu/create-index client index-name)]
    (is (eu/index-exists? client index-name))
    (is (not (eu/index-exists? client "wrongindex")))
    )
)

