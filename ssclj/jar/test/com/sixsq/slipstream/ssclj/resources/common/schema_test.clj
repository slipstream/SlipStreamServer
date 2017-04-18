(ns com.sixsq.slipstream.ssclj.resources.common.schema-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.common.schema :refer :all]
    [clojure.set :as set]))

(deftest check-actions
  (is (= (set/union core-actions prefixed-actions impl-prefixed-actions) (set (keys action-uri))))
  (is (= (set (map name core-actions)) (set (vals (select-keys action-uri core-actions))))))

