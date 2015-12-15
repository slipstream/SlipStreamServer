(ns com.sixsq.slipstream.auth.app.auth-service
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]

    [clj-http.client :as http]

    [com.sixsq.slipstream.auth.core :as auth]
    [com.sixsq.slipstream.auth.simple-authentication :as sa]
    [com.sixsq.slipstream.auth.app.http-utils :as hu]))

;; TODO : make this configurable
;; implementation chosen at run-time
(def authentication (sa/get-instance))

(defn- select-in-params
  [request keys]
  (-> request
      :params
      (select-keys keys)))

(defn- param-value
  [request key]
  (-> request
      :params
      (get key)))

(defn- extract-credentials
  [request]
  (select-in-params request [:user-name :password]))

(defn- extract-claims-token
  [request]
  (-> request
      (select-in-params [:claims :token])
      (update-in [:claims] #(json/read-str % :key-fn keyword))))

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

(defn dispatch-on-authn-method
  [request]
  (-> request
      (param-value :authn-method)
      keyword))

(defmulti login dispatch-on-authn-method)

(defmethod login :internal
  [request]
  (log/info "Internal authentication")
  (let [credentials   (extract-credentials request)
        [ok? result]  (auth/token authentication credentials)]
    (log-result credentials ok?)
    (if ok?
      (response-token-ok result)
      (response-invalid-token))))

(defmethod login :github
  [_]
  (log/info "Github authentication.")
  (hu/response-redirect
    307
    ;; TODO build URL with client id
    "https://github.com/login/oauth/authorize?client_id=cd03c88b13517f931f09&scope=user:email"))

;; TODO code specific to github should be in dedicated namespace
(defn callback-github
  [request]
  (let [oauth-code              (param-value request :code)
        access-token-response   (http/post
                                  "https://github.com/login/oauth/access_token"
                                  {:headers      {"Accept"        "application/json"}
                                   :form-params  {:client_id      "cd03c88b13517f931f09"
                                                  :client_secret  "6435cde7c22f0d543b07f13e311e0514c33460ad"
                                                  :code           oauth-code}})
        access-token            (-> access-token-response
                                    :body
                                    (json/read-str :key-fn keyword)
                                    :access_token)

        user-info               (http/get
                                  "https://api.github.com/user"
                                  {:headers      {"Authorization" (str "token " access-token)}})]

    (clojure.pprint/pprint user-info)

    (hu/response-with-body 200 (str "login ok for : " (:login (json/read-str (:body user-info) :key-fn keyword))))))
    ;; TODO : match user information from github with what's in DB
    ;; and returns same token as internal authentication

(defn build-token
  [request]
  (log/info "Will build-token")
  (let [{:keys [claims token]}  (extract-claims-token request)
        [ok? result]            (auth/token authentication claims token)]
    (if ok?
      (hu/response-with-body 200 (:token result))
      (response-invalid-token))))