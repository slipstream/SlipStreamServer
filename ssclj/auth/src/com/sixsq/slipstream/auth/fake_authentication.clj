(ns com.sixsq.slipstream.auth.fake-authentication
  (:require [com.sixsq.slipstream.auth.core :as core]))

;; Dummy implementation of AuthenticationServer for tests purpose

(deftype FakeAuthentication
  []
  core/AuthenticationServer

  (add-user
    [this user])

  (auth-user
    [this credentials]
    ))