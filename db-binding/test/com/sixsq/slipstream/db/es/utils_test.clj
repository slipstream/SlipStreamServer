(ns com.sixsq.slipstream.db.es.utils-test
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.es.utils :as eu]))

(deftest test-with-es-test-client
  (eu/with-es-test-client
    (is client)
    (is node)
    (is index)
    (is (eu/index-exists? client index))))

(deftest test-create-index-and-index-exists?
  (eu/with-es-test-client
    (let [lowercases (map char (range 97 122))
          index-name-length (inc (rand-int 100))            ;; avoid zero length
          index-name (reduce str (take index-name-length (repeatedly #(rand-nth lowercases))))
          res (eu/create-index client index-name)]
      (is (eu/index-exists? client index-name))
      (is (not (eu/index-exists? client "WrongIndex"))))))

