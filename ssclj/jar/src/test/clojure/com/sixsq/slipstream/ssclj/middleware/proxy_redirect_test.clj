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

(def old-host "http://localhost:8080")
(def new-host "http://localhost:8201")

(deftest rewrite-location-keeps-scheme
  (is (= "https://localhost:8201/user/MvAdrichem"
         (rewrite-location "https://localhost:8080/user/MvAdrichem" old-host new-host)))
  (is (= "http://localhost:8201/user/MvAdrichem"
         (rewrite-location "http://localhost:8080/user/MvAdrichem" old-host new-host))))

(deftest rewrite-location-defaults-to-dashboard
    (is (= "http://localhost:8201/dashboard"
           (rewrite-location "http://localhost:8080" old-host new-host))))

(deftest rewrite-location-nil-behavior
         (is (= "a" (rewrite-location "a" nil "c")))
         (is (= "a" (rewrite-location "a" "b" nil)))
         (is (= "" (rewrite-location nil nil "c")))
         (is (= "" (rewrite-location nil "b" nil)))
         (is (= "c/dashboard" (rewrite-location nil "b" "c"))))

(deftest no-location-header-creation
         (let [response {:headers {"dummy" "value"}}]
              (is (= response (update-location-header response
                                                      "http://myhost.example.org"
                                                      "http://myhost.example.org")))))

(deftest location-header-updated
         (let [response {:headers {"location" "http://myhost.example.org"
                                   "host" "myhost.example.org"}}]
              (is (= {:headers {"location" "http://myhost.example.org/dashboard"
                                "host" "myhost.example.org"}} (update-location-header response
                                                                                      "http://myhost.example.org"
                                                                                      "http://myhost.example.org")))))
