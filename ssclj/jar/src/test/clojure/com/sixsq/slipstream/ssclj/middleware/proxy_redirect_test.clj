(ns com.sixsq.slipstream.ssclj.middleware.proxy-redirect-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.middleware.proxy-redirect :refer :all]))

(deftest test-to-query-params
  (is (= {} (to-query-params "")))
  (is (= {} (to-query-params nil)))
  (is (= {"edit" "true"} (to-query-params "edit=true")))
  (is (= {"edit" "true" "display" "none"} (to-query-params "edit=true&display=none")))
  (is (= {"edit" "true" "display" ""} (to-query-params "edit=true&display="))))

(deftest test-rewrite-location
  (let [old "http://localhost:8080"
        new "http://localhost:8201"]

    (is (= "http://localhost:8201/user/MvAdrichem"
           (rewrite-location "http://localhost:8080/user/MvAdrichem" old new)))

    (is (= "http://localhost:8201/dashboard"
           (rewrite-location "http://localhost:8080" old new)))))
