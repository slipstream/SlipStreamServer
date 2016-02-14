(ns com.sixsq.slipstream.ssclj.middleware.logger-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.middleware.logger :refer :all]))

(deftest log-does-not-display-password
  (is (=  "200 (2142 ms) GET auth/login [super ADMIN] ?a=1&b=2 abcdef"
          (display-response
            { :request-method :get
              :uri            "auth/login"
              :headers        {"slipstream-authn-info" "super ADMIN"}
              :query-string   "a=1&password=secret&b=2"
              :body           "abcdef"}
            { :status 200}
            1450701200947
            1450701203089))))
