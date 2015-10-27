(ns com.sixsq.slipstream.auth.app.auth-service
  (:require
    [clojure.tools.logging :as log]

    [com.sixsq.slipstream.auth.core :as auth]
    [com.sixsq.slipstream.auth.simple-authentication :as sa]
    [com.sixsq.slipstream.auth.app.http-utils :as hu]
    ))

;; TODO : make this configurable
;; implementation chosen at run-time
(def authentication (sa/get-instance))

(defn- select-in-params
  [request keys]
  (-> request
      :params
      (select-keys keys)))

(defn- extract-credentials
  [request]
  (select-in-params request [:user-name :password]))

(defn- log-result
  [credentials ok?]
  (log/info (str "'" (:user-name credentials) "' : "
                 (if ok? "login OK" "invalid password"))))

(defn- response-token-ok
  [token]
  (-> (hu/response 200)
      (assoc :cookies {"com.sixsq.slipstream.cookie" {:value token}})))

(defn- response-invalid-token
  []
  (hu/response 401))

(defn login
  [request]
  (let [credentials (extract-credentials request)
        [ok? result] (auth/token authentication credentials)]
    (log-result credentials ok?)
    (if ok?
      (response-token-ok result)
      (response-invalid-token))))