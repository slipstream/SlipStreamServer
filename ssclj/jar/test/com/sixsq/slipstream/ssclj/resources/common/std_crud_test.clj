(ns com.sixsq.slipstream.ssclj.resources.common.std-crud-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :refer [resolve-href-keep href-not-found-msg]])
  (:import (clojure.lang ExceptionInfo)))

(deftest resolve-href-keep-with-nil-href
  (with-redefs [com.sixsq.slipstream.db.impl/retrieve (fn [_ _] nil)]
    (is (thrown-with-msg? ExceptionInfo (re-pattern href-not-found-msg)
                          (resolve-href-keep {:href "foo"} {})))))

