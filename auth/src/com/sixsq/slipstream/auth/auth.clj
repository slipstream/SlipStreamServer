(ns com.sixsq.slipstream.auth.auth
  (:refer-clojure :exclude [update])
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.internal-authentication :as ia]
    [com.sixsq.slipstream.auth.utils.http :as uh]))

(defn- extract-credentials
  [request]
  (uh/select-in-params request [:user-name :password]))

(defn- extract-claims-token
  [request]
  (-> request
      (uh/select-in-params [:claims :token])
      (update-in [:claims] #(json/read-str % :key-fn keyword))))

(defn- log-result
  [credentials ok?]
  (log/info (str "'" (:user-name credentials) "' : "
                 (if ok? "login OK" "invalid password"))))

(defn- response-token-ok
  [token]
  (-> (uh/response 200)
      (assoc :cookies {"com.sixsq.slipstream.cookie" {:value token}})))

(defn- response-invalid-token
  []
  (uh/response 401))

(defn dispatch-on-authn-method
  [request]
  (-> request
      (uh/param-value :authn-method)
      keyword))

(defmulti login dispatch-on-authn-method)

(defmethod login :internal
  [request]
  (log/info "Internal authentication")
  (let [credentials  (extract-credentials request)
        [ok? token]  (ia/create-token credentials)]
    (log-result credentials ok?)
    (if ok?
      (response-token-ok token)
      (response-invalid-token))))

(defmethod login :github
  [_]
  (log/info "Github authentication.")
  (uh/response-redirect
    ;; TODO build URL with client id
    "https://github.com/login/oauth/authorize?client_id=cd03c88b13517f931f09&scope=user:email"))

(defn build-token
  [request]
  (log/info "Will build-token")
  (let [{:keys [claims token]}  (extract-claims-token request)
        [ok? token]            (ia/create-token claims token)]
    (if ok?
      (uh/response-with-body 200 (:token token))
      (response-invalid-token))))