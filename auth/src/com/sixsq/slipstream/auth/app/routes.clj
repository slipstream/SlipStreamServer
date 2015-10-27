(ns com.sixsq.slipstream.auth.app.routes
  (:require
    [compojure.core :refer [defroutes let-routes routes POST GET PUT DELETE ANY]]
    [com.sixsq.slipstream.auth.app.auth-service :as as]))

(def auth-base-url "/auth")

(def uri-register     (str auth-base-url "/register"))
(def uri-login        (str auth-base-url "/login"))
(def uri-token        (str auth-base-url "/token"))

(def auth-routes
  (let-routes [uri-login (str auth-base-url "/login")]
    (POST uri-login request (as/login request))))