(ns com.sixsq.slipstream.auth.github
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [clj-http.client :as http]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.sign :as sg]))

(defn- map-slipstream-github!
  [slipstream-username github-login]
  (db/update-user-authn-info slipstream-username "github" github-login))

(defn- create-slipstream-user-from-github!
  [github-user-info]
  (db/create-user "github" (:login github-user-info) (:email github-user-info)))

(defn parse-github-user
  [user-info-body]
  (-> user-info-body
      (json/read-str :key-fn keyword)
      (select-keys [:login :email :name])))

(defn- match-github-user
  [github-user-info]
  (if-let [user-name-mapped (db/find-username-by-authn "github" (:login github-user-info))]
    user-name-mapped
    (let [user-names-same-email (db/find-usernames-by-email (:email github-user-info))]
      (condp = (count user-names-same-email)
        0 (create-slipstream-user-from-github! github-user-info)
        1 (map-slipstream-github! (first user-names-same-email) (:login github-user-info))
        ;; TODO multiple emails match github email
        "joe"))))

(defn callback-github
  [request redirect-server]
  (let [oauth-code            (uh/param-value request :code)
        access-token-response (http/post "https://github.com/login/oauth/access_token"
                                         {:headers     {"Accept" "application/json"}
                                          :form-params {:client_id     "cd03c88b13517f931f09"
                                                        :client_secret "6435cde7c22f0d543b07f13e311e0514c33460ad"
                                                        :code          oauth-code}})
        access-token          (-> access-token-response
                                  :body
                                  (json/read-str :key-fn keyword)
                                  :access_token)

        user-info-response    (http/get "https://api.github.com/user"
                                        {:headers {"Authorization" (str "token " access-token)}})

        user-info             (-> user-info-response :body parse-github-user)

        matched-user          (match-github-user user-info)

        token                 (sg/sign-claims {:com.sixsq.identifier matched-user :exp (sg/expiry-timestamp)})]

    (log/info "github user-info" user-info)

    (-> (uh/response-redirect (str redirect-server "/dashboard"))
        (assoc :cookies {"com.sixsq.slipstream.cookie" {:value {:token token}
                                                        :path  "/"}}))))