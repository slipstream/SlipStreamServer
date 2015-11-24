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

(deftest check-update-location
         (is (= "https://example.org/"
                (update-location "http://localhost" "https://example.org")))
         (is (= "https://example.org/appstore"
                (update-location "http://localhost/appstore" "https://example.org")))
         (is (= "https://example.org/appstore?query=3"
                (update-location "http://localhost/appstore?query=3" "https://example.org")))
         (is (= "https://example.org/appstore#myfrag"
                (update-location "http://localhost/appstore#myfrag" "https://example.org")))
         (is (= "https://example.org/"
                (update-location "http://localhost" "https://example.org/")))
         (is (= "https://example.org/appstore"
                (update-location "http://localhost/appstore" "https://example.org/")))
         (is (= "https://example.org/appstore?query=3"
                (update-location "http://localhost/appstore?query=3" "https://example.org/")))
         (is (= "https://example.org/appstore#myfrag"
                (update-location "http://localhost/appstore#myfrag" "https://example.org/"))))

(deftest no-location-header-creation
         (let [response {:headers {"dummy" "value"}}]
              (is (= response
                     (update-location-header response "http://myhost.example.org")))))

(deftest location-header-updated
         (let [response {:headers {"location" "http://myhost.example.org"
                                   "host" "myhost.example.org"}}]
              (is (= {:headers {"location" "http://myhost.example.org/"
                                "host" "myhost.example.org"}}
                     (update-location-header response "http://myhost.example.org")))))
