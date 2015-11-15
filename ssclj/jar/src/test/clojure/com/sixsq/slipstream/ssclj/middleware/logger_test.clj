(ns com.sixsq.slipstream.ssclj.middleware.logger-test
  (require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer :all]))

;; FIXME: re-enable this test after authentication is debugged
#_(deftest log-does-not-display-password
  (is (=  "GET auth/login [super ADMIN] ?a=1&b=2 abcdef"
          (request-to-str
            { :request-method "GET"
              :uri "auth/login"
              :headers {"slipstream-authn-info" "super ADMIN"}
              :query-string "a=1&password=secret&b=2"
              :body "abcdef"}))))
