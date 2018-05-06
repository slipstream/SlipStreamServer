(ns sixsq.slipstream.prs.ring-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.cookies]
    [sixsq.slipstream.prs.ring]))

(deftest test-authenticated?
  (with-redefs [com.sixsq.slipstream.auth.cookies/extract-cookie-info (fn [_] nil)]
    (is (false? (#'sixsq.slipstream.prs.ring/authenticated? {}))))
  (with-redefs [com.sixsq.slipstream.auth.cookies/extract-cookie-info (fn [_] [])]
    (is (false? (#'sixsq.slipstream.prs.ring/authenticated? {}))))
  (with-redefs [com.sixsq.slipstream.auth.cookies/extract-cookie-info (fn [_] [""])]
    (is (#'sixsq.slipstream.prs.ring/authenticated? {}))))
