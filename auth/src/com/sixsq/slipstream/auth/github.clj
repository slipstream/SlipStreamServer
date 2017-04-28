(ns com.sixsq.slipstream.auth.github
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [clj-http.client :as http]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.config :as cf]
    [com.sixsq.slipstream.auth.external :as ex]))

(defn parse-github-user
  [user-info]
  (-> user-info
      :body
      (json/read-str :key-fn keyword)
      (select-keys [:login :email])))

(defn primary-or-verified
  "Return primary verified email (if found) else fallbacks to any (non deterministic) verified email"
  [emails]
  (let [verified (filter :verified emails)]
    (if-let [primary (first (filter :primary verified))]
      (:email primary)
      (:email (first verified)))))

(defn- retrieve-private-email
  [access-token]
  (let [user-emails-response (http/get "https://api.github.com/user/emails"
                                       {:headers {"Authorization" (str "token " access-token)}})
        user-emails (-> user-emails-response :body (json/read-str :key-fn keyword))]
    (primary-or-verified user-emails)))

(defn- retrieve-email
  [user-info access-token]
  (if-let [public-email (:email user-info)]
    public-email
    (retrieve-private-email access-token)))

(defn- sanitized-login
  [user-info]
  (-> user-info :login ex/sanitize-login-name))

(defn get-github-access-token
  [client-id client-secret oauth-code]
  (-> (http/post "https://github.com/login/oauth/access_token"
                 {:headers     {"Accept" "application/json"}
                  :form-params {:client_id     client-id
                                :client_secret client-secret
                                :code          oauth-code}})
      :body
      (json/read-str :key-fn keyword)
      :access_token))

(defn get-github-user-info
  [access-token]
  (-> (http/get "https://api.github.com/user"
                {:headers {"Authorization" (str "token " access-token)}})
      (parse-github-user)))

(defn callback-github
  [request redirect-server]
  (try
    (let [oauth-code (uh/param-value request :code)

          access-token (get-github-access-token (cf/mandatory-property-value :github-client-id)
                                                (cf/mandatory-property-value :github-client-secret)
                                                oauth-code)

          user-info (get-github-user-info access-token)]

      (log/debug "Github user-info " user-info)
      (log/info "Successful GitHub authentication: " (sanitized-login user-info))
      (ex/redirect-with-matched-user :github
                                     (sanitized-login user-info)
                                     (retrieve-email user-info access-token)
                                     redirect-server))

    (catch Exception e
      (log/error "Invalid Github authentication " e)
      (uh/response-redirect (str redirect-server "/dashboard")))))

(defn login
  []
  (log/debug "Starting GitHub authentication.")
  (uh/response-redirect
    (format
      "https://github.com/login/oauth/authorize?client_id=%s&scope=user:email"
      (cf/mandatory-property-value :github-client-id))))

