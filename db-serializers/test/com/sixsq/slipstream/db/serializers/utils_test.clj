(ns com.sixsq.slipstream.db.serializers.utils-test
  (:require
    [clojure.test :refer [deftest is]]
    [com.sixsq.slipstream.db.serializers.utils :as u]))

(deftest test-as-boolean
  (is (true? (u/as-boolean true)))
  (is (false? (u/as-boolean false)))
  (is (true? (u/as-boolean "true")))
  (is (false? (u/as-boolean "false")))
  (is (nil? (u/as-boolean nil))))
