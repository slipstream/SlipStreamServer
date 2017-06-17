(ns com.sixsq.slipstream.ssclj.resources.session-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.session :as t]))

(deftest check-is-content-type?
  (are [expected-fn input] (expected-fn (t/is-content-type? input))
                           true? :content-type
                           true? "content-type"
                           true? "Content-Type"
                           true? "CONTENT-TYPE"
                           true? "CoNtEnT-TyPe"
                           false? 1234
                           false? nil))

(deftest check-is-form?
  (are [expected-fn input] (expected-fn (t/is-form? input))
                           true? {:content-type t/form-urlencoded}
                           true? {"content-type" t/form-urlencoded}
                           false? {:content-type "application/json"}
                           false? {"content-type" "application/json"}))

(deftest check-convert-form
  (is (= {:sessionTemplate {:alpha "alpha", :beta "beta"}}
         (t/convert-form {:alpha "alpha", "beta" "beta"}))))
